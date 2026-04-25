package com.nexus.agent.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis会话存储（有Redis时使用）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "agent:session:";
    private static final String MESSAGES_KEY_PREFIX = "agent:messages:";
    private static final long SESSION_EXPIRE_HOURS = 24;

    @Override
    public String createOrGetSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", sessionId);
            sessionData.put("createTime", new Date());
            redisTemplate.opsForHash().putAll(sessionKey, sessionData);
            redisTemplate.expire(sessionKey, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("创建新会话: {}", sessionId);
        }

        return sessionId;
    }

    @Override
    public void addMessage(String sessionId, ConversationMessage message) {
        if (sessionId == null) return;

        String messagesKey = MESSAGES_KEY_PREFIX + sessionId;
        message.setCreateTime(new Date());
        message.setId(UUID.randomUUID().toString().substring(0, 8));

        redisTemplate.opsForList().rightPush(messagesKey, message);
        redisTemplate.expire(messagesKey, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

        log.debug("添加消息到会话: sessionId={}, role={}", sessionId, message.getRole());
    }

    @Override
    public List<ConversationMessage> getMessages(String sessionId) {
        if (sessionId == null) return List.of();

        String messagesKey = MESSAGES_KEY_PREFIX + sessionId;
        List<Object> objects = redisTemplate.opsForList().range(messagesKey, 0, -1);

        if (objects == null) return List.of();

        List<ConversationMessage> messages = new ArrayList<>();
        for (Object obj : objects) {
            if (obj instanceof ConversationMessage) {
                messages.add((ConversationMessage) obj);
            }
        }

        return messages;
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

        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        redisTemplate.delete(MESSAGES_KEY_PREFIX + sessionId);
        log.debug("清除会话: {}", sessionId);
    }

    @Override
    public void updateContext(String sessionId, String key, Object value) {
        if (sessionId == null) return;

        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, key, value);
    }

    @Override
    public Object getContext(String sessionId, String key) {
        if (sessionId == null) return null;

        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        return redisTemplate.opsForHash().get(sessionKey, key);
    }

    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 12);
    }
}