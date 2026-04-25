package com.nexus.agent.memory;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 对话消息
 */
@Data
public class ConversationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private String id;

    /**
     * 角色：USER / AGENT / SYSTEM
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 工具调用（如果有）
     */
    private String toolCalls;

    /**
     * 工具调用结果（如果有）
     */
    private String toolResults;

    /**
     * 创建时间
     */
    private Date createTime;
}