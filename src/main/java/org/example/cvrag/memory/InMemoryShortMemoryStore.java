package org.example.cvrag.memory;

import org.example.cvrag.domain.ConversationTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "rag.memory", name = "redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryShortMemoryStore implements ShortMemoryStore {

    private final Map<String, List<ConversationTurn>> store = new ConcurrentHashMap<>();

    @Override
    public List<ConversationTurn> getTurns(String userId, String sessionId) {
        return new ArrayList<>(store.getOrDefault(key(userId, sessionId), List.of()));
    }

    @Override
    public void appendTurn(String userId, String sessionId, ConversationTurn turn) {
        store.computeIfAbsent(key(userId, sessionId), k -> new ArrayList<>()).add(turn);
    }

    @Override
    public void overwriteTurns(String userId, String sessionId, List<ConversationTurn> turns) {
        store.put(key(userId, sessionId), new ArrayList<>(turns));
    }

    @Override
    public List<SessionOverview> listSessions(String userId, int limit) {
        List<SessionOverview> sessions = new ArrayList<>();
        String prefix = userId + "::";
        for (Map.Entry<String, List<ConversationTurn>> entry : store.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            String sessionId = entry.getKey().substring(prefix.length());
            List<ConversationTurn> turns = entry.getValue();
            sessions.add(toOverview(sessionId, turns));
        }
        sessions.sort(Comparator.comparing(SessionOverview::lastUpdated).reversed());
        return sessions.stream().limit(Math.max(1, limit)).toList();
    }

    @Override
    public void clearSession(String userId, String sessionId) {
        store.remove(key(userId, sessionId));
    }

    private String key(String userId, String sessionId) {
        return userId + "::" + sessionId;
    }

    private SessionOverview toOverview(String sessionId, List<ConversationTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return new SessionOverview(sessionId, 0, Instant.EPOCH, "");
        }
        ConversationTurn last = turns.get(turns.size() - 1);
        String lastUserMessage = "";
        for (int i = turns.size() - 1; i >= 0; i--) {
            ConversationTurn t = turns.get(i);
            if ("user".equalsIgnoreCase(t.role())) {
                lastUserMessage = t.content();
                break;
            }
        }
        return new SessionOverview(sessionId, turns.size(), last.createdAt(), lastUserMessage);
    }
}
