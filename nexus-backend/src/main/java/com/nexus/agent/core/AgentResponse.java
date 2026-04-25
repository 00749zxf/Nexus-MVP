package com.nexus.agent.core;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Agent响应模型
 */
@Data
public class AgentResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Agent回复内容
     */
    private String response;

    /**
     * 建议操作列表
     */
    private List<Suggestion> suggestions;

    /**
     * 工具调用记录
     */
    private List<ToolCallRecord> toolCalls;

    /**
     * 是否需要转人工
     */
    private boolean needHumanSupport;

    /**
     * 响应时间(ms)
     */
    private Long responseTime;

    /**
     * 建议操作
     */
    @Data
    public static class Suggestion implements Serializable {
        private static final long serialVersionUID = 1L;
        private String type;       // LINK, ACTION, TEXT
        private String text;       // 显示文本
        private String url;        // 链接地址（LINK类型）
        private String action;     // 操作标识（ACTION类型）
        private Map<String, Object> params;  // 操作参数
    }

    /**
     * 工具调用记录
     */
    @Data
    public static class ToolCallRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        private String toolName;
        private Map<String, Object> params;
        private boolean success;
        private Object data;
        private String message;
        private Long executionTime;
    }
}