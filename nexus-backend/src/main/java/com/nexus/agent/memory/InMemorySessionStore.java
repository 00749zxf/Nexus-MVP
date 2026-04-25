package com.nexus.agent.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版会话存储（无Redis时使用）
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RedisSessionStore.class)
public class InMemorySessionStore implements SessionStore {

    private final Map<String, Map<String, Object>> sessionData = new ConcurrentHashMap<>();
    private final Map<String, List<ConversationMessage>> sessionMessages = new ConcurrentHashMap<>();

    @Override
    public String createOrGetSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        if (!sessionData.containsKey(sessionId)) {
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("createTime", new Date());
            sessionData.put(sessionId, data);
            sessionMessages.put(sessionId, new ArrayList<>());
            log.debug("创建新会话: {}", sessionId);
        }

        return sessionId;
    }

    @Override
    public void addMessage(String sessionId, ConversationMessage message) {
        if (sessionId == null) return;

        List<ConversationMessage> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());
        message.setCreateTime(new Date());
        message.setId(UUID.randomUUID().toString().substring(0, 8));
        messages.add(message);

        log.debug("添加消息到会话: sessionId={}, role={}", sessionId, message.getRole());
    }

    @Override
    public List<ConversationMessage> getMessages(String sessionId) {
        if (sessionId == null) return List.of();
        return sessionMessages.getOrDefault(sessionId, List.of());
    }

    @Override
    public List<ConversationMessage> getRecentMessages(String sessionId, int limit) {
        List<ConversationMessage> allMessages = getMessages(sessionId);
        if (allMessages.size() <= limit) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - limit, allMessages.size());
    }

    @Override
    public void clearSession(String sessionId) {
        if (sessionId == null) return;

        sessionData.remove(sessionId);
        sessionMessages.remove(sessionId);
        log.debug("清除会话: {}", sessionId);
    }

    @Override
    public void updateContext(String sessionId, String key, Object value) {
        if (sessionId == null) return;

        Map<String, Object> data = sessionData.computeIfAbsent(sessionId, k -> new HashMap<>());
        data.put(key, value);
    }

    @Override
    public Object getContext(String sessionId, String key) {
        if (sessionId == null) return null;

        Map<String, Object> data = sessionData.get(sessionId);
        return data != null ? data.get(key) : null;
    }

    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 12);
    }
}