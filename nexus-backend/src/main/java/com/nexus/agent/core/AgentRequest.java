package com.nexus.agent.core;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Agent请求模型
 */
@Data
public class AgentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Agent类型
     */
    private AgentType agentType;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 上下文信息
     */
    private AgentContext context;

    /**
     * Agent类型枚举
     */
    public enum AgentType {
        CUSTOMER_SERVICE("智能客服"),
        OPERATION("运营助手"),
        RISK("风控Agent"),
        SEARCH("搜索助手"),
        RECOMMENDATION("推荐Agent");

        private final String description;

        AgentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}