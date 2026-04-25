package com.nexus.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * DeepSeek LLM客户端实现
 * 使用DeepSeek API进行对话生成
 */
@Slf4j
@Component
public class DeepSeekClient implements LLMClient {

    @Value("${nexus.agent.llm.deepseek.api-key:}")
    private String apiKey;

    @Value("${nexus.agent.llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${nexus.agent.llm.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${nexus.agent.llm.deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${nexus.agent.llm.deepseek.max-tokens:2048}")
    private Integer maxTokens;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProvider() {
        return "deepseek";
    }

    @Override
    public LLMResponse generate(LLMRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + "/chat/completions";
            log.debug("DeepSeek API请求: url={}, model={}", url, model);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody(), startTime);

        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            LLMResponse errorResponse = new LLMResponse();
            errorResponse.setContent("抱歉，AI服务暂时不可用。请稍后再试或联系人工客服。");
            errorResponse.setResponseTimeMs(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);

        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统消息
        if (request.getSystemPrompt() != null) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // 用户/助手消息
        for (LLMClient.Message msg : request.getMessages()) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole());
            messageMap.put("content", msg.getContent());

            // 如果有工具调用结果（tool角色）
            if ("tool".equals(msg.getRole()) && msg.getToolCallId() != null) {
                messageMap.put("tool_call_id", msg.getToolCallId());
            }

            // 如果有工具调用（assistant角色）
            if ("assistant".equals(msg.getRole()) && msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (LLMClient.ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> tcMap = new HashMap<>();
                    tcMap.put("id", tc.getId());
                    tcMap.put("type", tc.getType());
                    tcMap.put("function", Map.of(
                            "name", tc.getFunction().getName(),
                            "arguments", tc.getFunction().getArguments()
                    ));
                    toolCalls.add(tcMap);
                }
                messageMap.put("tool_calls", toolCalls);
            }

            messages.add(messageMap);
        }

        body.put("messages", messages);

        // 工具定义
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (LLMClient.ToolDefinition td : request.getTools()) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("type", td.getType());
                toolMap.put("function", Map.of(
                        "name", td.getFunction().getName(),
                        "description", td.getFunction().getDescription(),
                        "parameters", td.getFunction().getParameters()
                ));
                tools.add(toolMap);
            }
            body.put("tools", tools);
        }

        return body;
    }

    private LLMResponse parseResponse(String responseBody, long startTime) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        LLMResponse response = new LLMResponse();
        response.setResponseTimeMs(System.currentTimeMillis() - startTime);

        if (choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");

            // 获取内容
            String content = message.path("content").asText("");
            response.setContent(content);

            // 解析工具调用
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                List<LLMClient.ToolCall> calls = new ArrayList<>();
                for (JsonNode tc : toolCalls) {
                    LLMClient.ToolCall toolCall = new LLMClient.ToolCall();
                    toolCall.setId(tc.path("id").asText());
                    toolCall.setType(tc.path("type").asText("function"));

                    LLMClient.ToolCall.FunctionCall functionCall = new LLMClient.ToolCall.FunctionCall();
                    JsonNode function = tc.path("function");
                    functionCall.setName(function.path("name").asText());
                    functionCall.setArguments(function.path("arguments").asText());
                    toolCall.setFunction(functionCall);

                    calls.add(toolCall);
                }
                response.setToolCalls(calls);
            }
        }

        // 解析使用统计
        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            response.setUsagePromptTokens(usage.path("prompt_tokens").asInt(0));
            response.setUsageCompletionTokens(usage.path("completion_tokens").asInt(0));
        }

        log.debug("DeepSeek响应: contentLength={}, toolCalls={}, tokens={}",
                response.getContent() != null ? response.getContent().length() : 0,
                response.getToolCalls() != null ? response.getToolCalls().size() : 0,
                response.getUsagePromptTokens() + response.getUsageCompletionTokens());

        return response;
    }
}