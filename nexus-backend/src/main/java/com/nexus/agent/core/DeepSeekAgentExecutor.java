package com.nexus.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.agent.llm.DeepSeekClient;
import com.nexus.agent.llm.LLMClient;
import com.nexus.agent.memory.ConversationMessage;
import com.nexus.agent.memory.SessionStore;
import com.nexus.agent.prompts.PromptRegistry;
import com.nexus.agent.tools.Tool;
import com.nexus.agent.tools.ToolRegistry;
import com.nexus.agent.tools.ToolResult;
import com.nexus.agent.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DeepSeek Agent执行器
 * 使用DeepSeek API进行智能对话
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekAgentExecutor implements AgentExecutor {

    private final DeepSeekClient deepSeekClient;
    private final ToolRegistry toolRegistry;
    private final PromptRegistry promptRegistry;
    private final SessionStore sessionStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nexus.agent.max-tool-calls:5}")
    private int maxToolCalls;

    @Override
    public String generateResponse(AgentRequest request, Map<String, Object> toolResults) {
        // 这个方法不再直接使用，改为通过AgentEngine调用executeWithLLM
        return executeWithLLM(request, toolResults);
    }

    /**
     * 使用DeepSeek执行完整对话流程
     */
    public AgentResponse executeWithLLM(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString().substring(0, 12);

        try {
            // 初始化会话
            sessionId = sessionStore.createOrGetSession(sessionId);

            // 添加用户消息到记忆
            ConversationMessage userMessage = new ConversationMessage();
            userMessage.setRole("USER");
            userMessage.setContent(request.getMessage());
            sessionStore.addMessage(sessionId, userMessage);

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(request);

            // 获取历史消息
            List<ConversationMessage> history = sessionStore.getRecentMessages(sessionId, 10);
            List<LLMClient.Message> messages = buildMessages(history);

            // 构建工具定义
            List<LLMClient.ToolDefinition> tools = buildToolDefinitions(request);

            // 调用LLM（可能包含多轮工具调用）
            AgentResponse response = callLLMWithTools(sessionId, systemPrompt, messages, tools, request);

            response.setSessionId(sessionId);
            response.setResponseTime(System.currentTimeMillis() - startTime);

            return response;

        } catch (Exception e) {
            log.error("DeepSeek执行失败: sessionId={}", sessionId, e);

            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSessionId(sessionId);
            errorResponse.setResponse("抱歉，处理您的请求时出现错误。请稍后再试或联系人工客服。");
            errorResponse.setNeedHumanSupport(true);
            errorResponse.setResponseTime(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AgentRequest request) {
        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("currentTime", new Date().toString());
        contextVars.put("agentType", request.getAgentType().getDescription());

        // 添加用户身份信息
        AgentContext ctx = request.getContext();
        if (ctx != null) {
            if (ctx.getMemberId() != null) {
                contextVars.put("memberId", ctx.getMemberId().toString());
            }
            if (ctx.getUsername() != null) {
                contextVars.put("username", ctx.getUsername());
            }
            if (ctx.getCurrentPage() != null) {
                contextVars.put("currentPage", ctx.getCurrentPage());
            }
        }

        String prompt = promptRegistry.getCustomerServicePrompt(contextVars);

        // 追加用户身份提示（如果已知）
        if (ctx != null && (ctx.getMemberId() != null || ctx.getUsername() != null)) {
            StringBuilder identitySection = new StringBuilder("\n\n---\n## 当前用户身份\n");
            if (ctx.getMemberId() != null) {
                identitySection.append("- 用户ID: ").append(ctx.getMemberId()).append("\n");
            }
            if (ctx.getUsername() != null) {
                identitySection.append("- 用户名: ").append(ctx.getUsername()).append("\n");
            }
            identitySection.append("\n你已知道用户的身份信息。当用户问\"我是谁\"时，直接告诉用户其ID和用户名。");
            prompt += identitySection.toString();
        }

        return prompt;
    }

    /**
     * 构建消息列表
     */
    private List<LLMClient.Message> buildMessages(List<ConversationMessage> history) {
        return history.stream()
                .map(h -> {
                    LLMClient.Message msg = new LLMClient.Message();
                    String role = h.getRole();
                    // 转换角色名：USER -> user, AGENT -> assistant
                    msg.setRole(role.equals("USER") ? "user" : role.equals("AGENT") ? "assistant" : role.toLowerCase());
                    msg.setContent(h.getContent());
                    return msg;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建工具定义
     */
    private List<LLMClient.ToolDefinition> buildToolDefinitions(AgentRequest request) {
        List<Tool> availableTools = getAvailableTools(request.getAgentType());

        return availableTools.stream()
                .map(tool -> {
                    LLMClient.ToolDefinition td = new LLMClient.ToolDefinition();
                    td.setType("function");

                    LLMClient.ToolDefinition.FunctionDef fd = new LLMClient.ToolDefinition.FunctionDef();
                    fd.setName(tool.getName());
                    fd.setDescription(tool.getDescription());
                    fd.setParameters(buildToolParametersSchema(tool.getSchema()));

                    td.setFunction(fd);
                    return td;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建工具参数Schema
     */
    private Map<String, Object> buildToolParametersSchema(ToolSchema schema) {
        Map<String, Object> paramsSchema = new HashMap<>();
        paramsSchema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        if (schema != null && schema.getParams() != null) {
            for (ToolSchema.ParamDef param : schema.getParams()) {
                Map<String, Object> prop = new HashMap<>();
                prop.put("type", param.getType());
                prop.put("description", param.getDescription());
                properties.put(param.getName(), prop);

                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }
        }

        paramsSchema.put("properties", properties);
        paramsSchema.put("required", required);

        return paramsSchema;
    }

    /**
     * 获取可用工具列表
     */
    private List<Tool> getAvailableTools(AgentRequest.AgentType agentType) {
        // 根据Agent类型返回可用工具
        List<String> toolNames = switch (agentType) {
            case CUSTOMER_SERVICE -> List.of("queryProduct", "queryOrder", "queryUser", "queryCart");
            case OPERATION -> List.of("queryProduct", "queryOrder", "queryUser");
            case RISK -> List.of("queryOrder", "queryUser");
            case SEARCH -> List.of("queryProduct");
            case RECOMMENDATION -> List.of("queryProduct");
        };

        return toolRegistry.getTools(toolNames);
    }

    /**
     * 调用LLM并处理工具调用
     */
    private AgentResponse callLLMWithTools(String sessionId, String systemPrompt,
                                            List<LLMClient.Message> messages,
                                            List<LLMClient.ToolDefinition> tools,
                                            AgentRequest request) {

        List<AgentResponse.ToolCallRecord> toolCallRecords = new ArrayList<>();
        int toolCallCount = 0;
        String finalContent = null;

        while (toolCallCount < maxToolCalls) {
            // 构建LLM请求
            LLMClient.LLMRequest llmRequest = new LLMClient.LLMRequest();
            llmRequest.setSystemPrompt(systemPrompt);
            llmRequest.setMessages(messages);
            llmRequest.setTools(tools);
            llmRequest.setTemperature(0.7);
            llmRequest.setMaxTokens(2048);

            // 调用DeepSeek
            LLMClient.LLMResponse llmResponse = deepSeekClient.generate(llmRequest);

            // 检查是否有工具调用
            if (llmResponse.getToolCalls() != null && !llmResponse.getToolCalls().isEmpty()) {
                toolCallCount++;

                // 处理工具调用
                for (LLMClient.ToolCall tc : llmResponse.getToolCalls()) {
                    // 执行工具
                    Map<String, Object> params = parseToolArguments(tc.getFunction().getArguments());
                    ToolResult result = toolRegistry.execute(tc.getFunction().getName(), params);

                    // 记录工具调用
                    AgentResponse.ToolCallRecord record = new AgentResponse.ToolCallRecord();
                    record.setToolName(tc.getFunction().getName());
                    record.setParams(params);
                    record.setSuccess(result.isSuccess());
                    record.setData(result.getData());
                    record.setMessage(result.getMessage());
                    toolCallRecords.add(record);

                    // 添加助手消息（带工具调用）到历史
                    LLMClient.Message assistantMsg = new LLMClient.Message();
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(llmResponse.getContent() != null ? llmResponse.getContent() : "");
                    assistantMsg.setToolCalls(List.of(tc));
                    messages.add(assistantMsg);

                    // 添加工具结果消息
                    LLMClient.Message toolMsg = new LLMClient.Message();
                    toolMsg.setRole("tool");
                    toolMsg.setToolCallId(tc.getId());
                    try {
                        toolMsg.setContent(objectMapper.writeValueAsString(result.getData()));
                    } catch (Exception e) {
                        toolMsg.setContent(result.getMessage());
                    }
                    messages.add(toolMsg);

                    log.debug("工具调用: {} -> success={}", tc.getFunction().getName(), result.isSuccess());
                }

            } else {
                // 没有工具调用，获取最终回复
                finalContent = llmResponse.getContent();
                break;
            }
        }

        // 如果达到最大工具调用次数但没有最终内容，生成一个总结
        if (finalContent == null) {
            finalContent = generateSummaryFromToolResults(toolCallRecords, request);
        }

        // 保存助手回复到记忆
        ConversationMessage assistantMessage = new ConversationMessage();
        assistantMessage.setRole("AGENT");
        assistantMessage.setContent(finalContent);
        sessionStore.addMessage(sessionId, assistantMessage);

        // 构建响应
        AgentResponse response = new AgentResponse();
        response.setResponse(finalContent);
        response.setToolCalls(toolCallRecords);
        response.setSuggestions(generateSuggestions(toolCallRecords));
        response.setNeedHumanSupport(shouldTransferToHuman(finalContent));

        return response;
    }

    /**
     * 解析工具参数
     */
    private Map<String, Object> parseToolArguments(String argumentsJson) {
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (Exception e) {
            log.warn("解析工具参数失败: {}", argumentsJson);
            return new HashMap<>();
        }
    }

    /**
     * 根据工具结果生成总结
     */
    private String generateSummaryFromToolResults(List<AgentResponse.ToolCallRecord> records, AgentRequest request) {
        StringBuilder sb = new StringBuilder();

        for (AgentResponse.ToolCallRecord record : records) {
            if (record.isSuccess() && record.getData() != null) {
                if (record.getToolName().equals("queryProduct") && record.getData() instanceof List) {
                    List<?> products = (List<?>) record.getData();
                    sb.append("为您找到 ").append(products.size()).append(" 个商品。\n");
                } else if (record.getToolName().equals("queryOrder")) {
                    sb.append("已查询到您的订单信息。\n");
                } else if (record.getToolName().equals("queryCart")) {
                    sb.append("已查询到您的购物车信息。\n");
                }
            }
        }

        if (sb.isEmpty()) {
            sb.append("根据查询结果，我已获取了相关信息。请问您还需要什么帮助？");
        }

        return sb.toString();
    }

    /**
     * 生成建议操作
     */
    private List<AgentResponse.Suggestion> generateSuggestions(List<AgentResponse.ToolCallRecord> toolCallRecords) {
        List<AgentResponse.Suggestion> suggestions = new ArrayList<>();

        for (AgentResponse.ToolCallRecord record : toolCallRecords) {
            if (record.getToolName().equals("queryOrder")) {
                AgentResponse.Suggestion suggestion = new AgentResponse.Suggestion();
                suggestion.setType("LINK");
                suggestion.setText("查看订单详情");
                suggestion.setUrl("/orders");
                suggestions.add(suggestion);
            }
            if (record.getToolName().equals("queryCart")) {
                AgentResponse.Suggestion suggestion = new AgentResponse.Suggestion();
                suggestion.setType("LINK");
                suggestion.setText("去购物车结算");
                suggestion.setUrl("/cart");
                suggestions.add(suggestion);
            }
        }

        // 添加转人工建议
        AgentResponse.Suggestion humanSupport = new AgentResponse.Suggestion();
        humanSupport.setType("ACTION");
        humanSupport.setText("转人工客服");
        humanSupport.setAction("transferToHuman");
        suggestions.add(humanSupport);

        return suggestions;
    }

    /**
     * 判断是否需要转人工
     */
    private boolean shouldTransferToHuman(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("无法") && lower.contains("建议")
                || lower.contains("人工客服")
                || lower.contains("转人工");
    }

    // 兼容旧接口
    private String executeWithLLM(AgentRequest request, Map<String, Object> toolResults) {
        AgentResponse response = executeWithLLM(request);
        return response.getResponse();
    }
}