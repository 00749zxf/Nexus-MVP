package com.nexus.agent.core;

import com.nexus.agent.tools.ToolRegistry;
import com.nexus.agent.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent执行引擎
 * 处理Agent请求，协调工具调用，生成响应
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEngine {

    private final ToolRegistry toolRegistry;
    private final DeepSeekAgentExecutor deepSeekExecutor;
    private final MockAgentExecutor mockExecutor;

    @Value("${nexus.agent.use-llm:false}")
    private boolean useLLM;

    /**
     * 执行Agent请求
     */
    public AgentResponse execute(AgentRequest request) {
        if (useLLM) {
            log.info("使用DeepSeek执行Agent请求");
            return deepSeekExecutor.executeWithLLM(request);
        } else {
            log.info("使用Mock执行Agent请求（测试模式）");
            return executeWithMock(request);
        }
    }

    private AgentResponse executeWithMock(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : generateSessionId();

        try {
            log.info("Agent请求(Mock): sessionId={}, type={}, message={}",
                    sessionId, request.getAgentType(), request.getMessage());

            List<ToolCallPlan> toolPlans = analyzeIntent(request);
            List<AgentResponse.ToolCallRecord> toolCallRecords = new ArrayList<>();
            Map<String, Object> toolResults = new HashMap<>();

            for (ToolCallPlan plan : toolPlans) {
                ToolResult result = toolRegistry.execute(plan.toolName, plan.params);

                AgentResponse.ToolCallRecord record = new AgentResponse.ToolCallRecord();
                record.setToolName(plan.toolName);
                record.setParams(plan.params);
                record.setSuccess(result.isSuccess());
                record.setData(result.getData());
                record.setMessage(result.getMessage());
                toolCallRecords.add(record);

                if (result.isSuccess()) {
                    toolResults.put(plan.toolName, result.getData());
                }
            }

            String response = mockExecutor.generateResponse(request, toolResults);
            List<AgentResponse.Suggestion> suggestions = generateSuggestions(request, toolResults);

            AgentResponse agentResponse = new AgentResponse();
            agentResponse.setSessionId(sessionId);
            agentResponse.setResponse(response);
            agentResponse.setSuggestions(suggestions);
            agentResponse.setToolCalls(toolCallRecords);
            agentResponse.setResponseTime(System.currentTimeMillis() - startTime);
            agentResponse.setNeedHumanSupport(false);

            return agentResponse;

        } catch (Exception e) {
            log.error("Agent执行失败", e);
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSessionId(sessionId);
            errorResponse.setResponse("抱歉，处理您的请求时出现错误。请稍后再试或联系人工客服。");
            errorResponse.setNeedHumanSupport(true);
            return errorResponse;
        }
    }

    private List<ToolCallPlan> analyzeIntent(AgentRequest request) {
        String message = request.getMessage().toLowerCase();
        AgentContext context = request.getContext();
        List<ToolCallPlan> plans = new ArrayList<>();

        if (containsAny(message, "商品", "产品", "价格", "库存", "多少钱", "product")) {
            ToolCallPlan plan = new ToolCallPlan();
            plan.toolName = "queryProduct";
            plan.params = new HashMap<>();
            plan.params.put("status", 1);
            plans.add(plan);
        }

        if (containsAny(message, "订单", "发货", "快递", "物流", "order")) {
            ToolCallPlan plan = new ToolCallPlan();
            plan.toolName = "queryOrder";
            plan.params = new HashMap<>();
            if (context != null && context.getMemberId() != null) {
                plan.params.put("memberId", context.getMemberId());
            }
            plans.add(plan);
        }

        if (containsAny(message, "购物车", "cart")) {
            ToolCallPlan plan = new ToolCallPlan();
            plan.toolName = "queryCart";
            plan.params = new HashMap<>();
            if (context != null && context.getMemberId() != null) {
                plan.params.put("memberId", context.getMemberId());
            }
            plans.add(plan);
        }

        if (containsAny(message, "用户", "我的", "user")) {
            ToolCallPlan plan = new ToolCallPlan();
            plan.toolName = "queryUser";
            plan.params = new HashMap<>();
            if (context != null && context.getMemberId() != null) {
                plan.params.put("memberId", context.getMemberId());
            }
            plans.add(plan);
        }

        if (plans.isEmpty()) {
            ToolCallPlan plan = new ToolCallPlan();
            plan.toolName = "queryProduct";
            plan.params = new HashMap<>();
            plan.params.put("status", 1);
            plans.add(plan);
        }

        return plans;
    }

    private List<AgentResponse.Suggestion> generateSuggestions(AgentRequest request, Map<String, Object> toolResults) {
        List<AgentResponse.Suggestion> suggestions = new ArrayList<>();

        if (toolResults.containsKey("queryOrder")) {
            AgentResponse.Suggestion suggestion = new AgentResponse.Suggestion();
            suggestion.setType("LINK");
            suggestion.setText("查看订单详情");
            suggestion.setUrl("/orders");
            suggestions.add(suggestion);
        }

        AgentResponse.Suggestion humanSupport = new AgentResponse.Suggestion();
        humanSupport.setType("ACTION");
        humanSupport.setText("转人工客服");
        humanSupport.setAction("transferToHuman");
        suggestions.add(humanSupport);

        return suggestions;
    }

    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static class ToolCallPlan {
        String toolName;
        Map<String, Object> params;
    }
}