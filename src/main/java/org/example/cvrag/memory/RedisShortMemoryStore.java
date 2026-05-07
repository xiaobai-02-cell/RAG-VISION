package org.example.cvrag.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ConversationTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "rag.memory", name = "redis-enabled", havingValue = "true")
public class RedisShortMemoryStore implements ShortMemoryStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;

    public RedisShortMemoryStore(StringRedisTemplate redis, ObjectMapper objectMapper, RagProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<ConversationTurn> getTurns(String userId, String sessionId) {
        String key = key(userId, sessionId);
        List<String> raw = redis.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ConversationTurn> turns = new ArrayList<>();
        for (String item : raw) {
            try {
                turns.add(objectMapper.readValue(item, ConversationTurn.class));
            } catch (JsonProcessingException e) {
                // Skip malformed entries to keep read path robust.
            }
        }
        return turns;
    }

    @Override
    public void appendTurn(String userId, String sessionId, ConversationTurn turn) {
        String key = key(userId, sessionId);
        try {
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(turn));
            redis.expire(key, Duration.ofHours(properties.getMemory().getTtlHours()));
            touchSession(userId, sessionId, turn.createdAt());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize conversation turn", e);
        }
    }

    @Override
    public void overwriteTurns(String userId, String sessionId, List<ConversationTurn> turns) {
        String key = key(userId, sessionId);
        redis.delete(key);
        for (ConversationTurn turn : turns) {
            appendTurn(userId, sessionId, turn);
        }
        if (turns.isEmpty()) {
            clearSession(userId, sessionId);
        }
    }

    @Override
    public List<SessionOverview> listSessions(String userId, int limit) {
        String indexKey = sessionIndexKey(userId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(indexKey, 0, Math.max(0, limit - 1));
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<SessionOverview> sessions = new ArrayList<>();
        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : tuples) {
            String sessionId = tuple.getValue();
            if (sessionId == null || sessionId.isBlank()) {
                continue;
            }
            List<ConversationTurn> turns = getTurns(userId, sessionId);
            if (turns.isEmpty()) {
                redis.opsForZSet().remove(indexKey, sessionId);
                continue;
            }
            String lastUserMessage = "";
            for (int i = turns.size() - 1; i >= 0; i--) {
                ConversationTurn t = turns.get(i);
                if ("user".equalsIgnoreCase(t.role())) {
                    lastUserMessage = t.content();
                    break;
                }
            }
            ConversationTurn last = turns.get(turns.size() - 1);
            sessions.add(new SessionOverview(sessionId, turns.size(), last.createdAt(), lastUserMessage));
        }
        return sessions;
    }

    @Override
    public void clearSession(String userId, String sessionId) {
        redis.delete(key(userId, sessionId));
        redis.opsForZSet().remove(sessionIndexKey(userId), sessionId);
    }

    private String key(String userId, String sessionId) {
        return "cv-rag:short-memory:" + userId + ":" + sessionId;
    }

    private String sessionIndexKey(String userId) {
        return "cv-rag:sessions:" + userId;
    }

    private void touchSession(String userId, String sessionId, Instant ts) {
        String indexKey = sessionIndexKey(userId);
        double score = ts == null ? Instant.now().toEpochMilli() : ts.toEpochMilli();
        redis.opsForZSet().add(indexKey, sessionId, score);
        redis.expire(indexKey, Duration.ofHours(properties.getMemory().getTtlHours()));
    }
}
