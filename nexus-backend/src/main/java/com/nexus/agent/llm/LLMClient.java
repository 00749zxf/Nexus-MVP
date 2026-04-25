package com.nexus.agent.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM客户端接口
 * 支持多种LLM提供商
 */
public interface LLMClient {

    /**
     * 获取提供商名称
     */
    String getProvider();

    /**
     * 生成回复
     */
    LLMResponse generate(LLMRequest request);

    /**
     * 流式生成回复（可选实现）
     */
    default void generateStream(LLMRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException("该LLM不支持流式输出");
    }

    /**
     * LLM请求
     */
    @lombok.Data
    public static class LLMRequest {
        private String systemPrompt;
        private List<Message> messages;
        private Double temperature;
        private Integer maxTokens;
        private List<ToolDefinition> tools;
    }

    /**
     * LLM响应
     */
    @lombok.Data
    public static class LLMResponse {
        private String content;
        private List<ToolCall> toolCalls;
        private Integer usagePromptTokens;
        private Integer usageCompletionTokens;
        private Long responseTimeMs;
    }

    /**
     * 消息
     */
    @lombok.Data
    public static class Message {
        private String role;  // system, user, assistant, tool
        private String content;
        private List<ToolCall> toolCalls;
        private String toolCallId;  // 用于tool角色的消息
    }

    /**
     * 工具定义
     */
    @lombok.Data
    public static class ToolDefinition {
        private String type;  // function
        private FunctionDef function;

        @lombok.Data
        public static class FunctionDef {
            private String name;
            private String description;
            private Map<String, Object> parameters;  // JSON Schema
        }
    }

    /**
     * 工具调用
     */
    @lombok.Data
    public static class ToolCall {
        private String id;
        private String type;  // function
        private FunctionCall function;

        @lombok.Data
        public static class FunctionCall {
            private String name;
            private String arguments;  // JSON格式的参数
        }
    }

    /**
     * 流式回调
     */
    public interface StreamCallback {
        void onToken(String token);
        void onToolCall(ToolCall toolCall);
        void onComplete(LLMResponse response);
        void onError(Exception e);
    }
}