package com.nexus.agent.core;

import com.nexus.agent.memory.ConversationMessage;
import com.nexus.agent.memory.SessionStore;
import com.nexus.agent.prompts.PromptRegistry;
import com.nexus.agent.service.RerankService;
import com.nexus.agent.tools.NexusSpringAITools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于 Spring AI 的 Agent 执行器
 * 替换原有手写 DeepSeek HTTP 客户端，接入 RAG 知识库和持久化对话记忆
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAIAgentExecutor implements AgentExecutor {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final NexusSpringAITools nexusTools;
    private final PromptRegistry promptRegistry;
    private final SessionStore sessionStore;
    private final RerankService rerankService;

    // 每个 sessionId 对应独立的 ChatClient（带记忆）
    private final Map<String, ChatClient> sessionClients = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String generateResponse(AgentRequest request, Map<String, Object> toolResults) {
        return executeWithLLM(request).getResponse();
    }

    public AgentResponse executeWithLLM(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString().substring(0, 12);

        try {
            sessionId = sessionStore.createOrGetSession(sessionId);
            // lambda 需要 effectively final 变量
            final String sid = sessionId;

            // 1. 从向量库检索相关知识
            String knowledge = searchKnowledge(request.getMessage());

            // 2. 构建 System Prompt（含知识库内容）
            String systemPrompt = buildSystemPrompt(request, knowledge);

            // 3. 获取或创建该 session 的 ChatClient（含持久化记忆）
            ChatClient chatClient = getOrCreateClient(sid);

            // 4. 调用 AI（所有 agent 类型均可使用 NexusSpringAITools，LLM 自行决策调用哪些）
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .tools(nexusTools)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sid))
                    .call()
                    .content();

            // 6. 同步到原有 SessionStore（兼容现有记忆体系）
            ConversationMessage userMsg = new ConversationMessage();
            userMsg.setRole("USER");
            userMsg.setContent(request.getMessage());
            sessionStore.addMessage(sessionId, userMsg);

            ConversationMessage agentMsg = new ConversationMessage();
            agentMsg.setRole("AGENT");
            agentMsg.setContent(response);
            sessionStore.addMessage(sessionId, agentMsg);

            AgentResponse agentResponse = new AgentResponse();
            agentResponse.setSessionId(sessionId);
            agentResponse.setResponse(response);
            agentResponse.setSuggestions(generateSuggestions());
            agentResponse.setNeedHumanSupport(shouldTransferToHuman(response));
            agentResponse.setResponseTime(System.currentTimeMillis() - startTime);
            return agentResponse;

        } catch (Exception e) {
            log.error("SpringAI Agent 执行失败: sessionId={}", sessionId, e);
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSessionId(sessionId);
            errorResponse.setResponse("抱歉，处理您的请求时出现错误，请稍后再试或联系人工客服。");
            errorResponse.setNeedHumanSupport(true);
            errorResponse.setResponseTime(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    private String searchKnowledge(String query) {
        try {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                log.debug("RAG 未启用，跳过知识库检索");
                return "";
            }

            // 第一步：向量检索，取 Top-5（多取几个给 Rerank 挑选）
            var results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(5)
                            .similarityThreshold(0.4)
                            .build()
            );
            if (results == null || results.isEmpty()) return "";

            // 第二步：Rerank 重排，从 5 个里选出最相关的 3 个
            List<String> candidates = results.stream()
                    .map(doc -> doc.getText())
                    .toList();
            List<String> reranked = rerankService.rerank(query, candidates, 3);

            if (reranked.isEmpty()) return "";
            return String.join("\n\n", reranked);

        } catch (Exception e) {
            log.warn("知识库检索失败，跳过 RAG: {}", e.getMessage());
            return "";
        }
    }

    private String buildSystemPrompt(AgentRequest request, String knowledge) {
        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("currentTime", new Date().toString());
        AgentRequest.AgentType agentType = request.getAgentType() != null
                ? request.getAgentType()
                : AgentRequest.AgentType.CUSTOMER_SERVICE;
        contextVars.put("agentType", agentType.getDescription());

        AgentContext ctx = request.getContext();
        if (ctx != null) {
            if (ctx.getMemberId() != null) contextVars.put("memberId", ctx.getMemberId().toString());
            if (ctx.getUsername() != null) contextVars.put("username", ctx.getUsername());
        }

        String basePrompt = promptRegistry.getCustomerServicePrompt(contextVars);

        if (ctx != null && (ctx.getMemberId() != null || ctx.getUsername() != null)) {
            basePrompt += "\n\n## 当前用户\n- 用户ID: " + ctx.getMemberId()
                    + "\n- 用户名: " + ctx.getUsername();
        }

        if (!knowledge.isEmpty()) {
            basePrompt += "\n\n## 知识库参考资料\n"
                    + "以下是与用户问题相关的资料，回答时必须遵守以下规则：\n"
                    + "1. 优先且严格基于以下资料回答，不得添加资料中没有的信息\n"
                    + "2. 如果资料中没有用户问题的答案，直接回复\"抱歉，这个问题我暂时没有相关资料，建议您联系人工客服\"\n"
                    + "3. 不要用自身训练知识补充或猜测资料之外的内容\n"
                    + "---------------------\n" + knowledge + "\n---------------------";
        } else {
            basePrompt += "\n\n## 注意\n当前没有找到与问题相关的知识库资料。"
                    + "如果问题涉及具体的产品规格、政策细节等专业内容，请回复\"抱歉，我暂时没有这方面的资料\"，不要猜测。";
        }

        return basePrompt;
    }

    private ChatClient getOrCreateClient(String sessionId) {
        return sessionClients.computeIfAbsent(sessionId, id -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(20)
                    .build();
            return chatClientBuilder
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                    .build();
        });
    }

    private List<AgentResponse.Suggestion> generateSuggestions() {
        List<AgentResponse.Suggestion> suggestions = new ArrayList<>();
        AgentResponse.Suggestion humanSupport = new AgentResponse.Suggestion();
        humanSupport.setType("ACTION");
        humanSupport.setText("转人工客服");
        humanSupport.setAction("transferToHuman");
        suggestions.add(humanSupport);
        return suggestions;
    }

    private boolean shouldTransferToHuman(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return (lower.contains("无法") && lower.contains("建议"))
                || lower.contains("人工客服")
                || lower.contains("转人工");
    }
}
