package com.nexus.agent.memory;

import java.util.List;

/**
 * 会话存储接口
 */
public interface SessionStore {

    /**
     * 创建或获取会话
     */
    String createOrGetSession(String sessionId);

    /**
     * 添加消息到会话
     */
    void addMessage(String sessionId, ConversationMessage message);

    /**
     * 获取会话的所有消息
     */
    List<ConversationMessage> getMessages(String sessionId);

    /**
     * 获取最近的N条消息
     */
    List<ConversationMessage> getRecentMessages(String sessionId, int limit);

    /**
     * 清除会话
     */
    void clearSession(String sessionId);

    /**
     * 更新会话上下文
     */
    void updateContext(String sessionId, String key, Object value);

    /**
     * 获取会话上下文
     */
    Object getContext(String sessionId, String key);
}