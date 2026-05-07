package org.example.cvrag.memory;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ConversationTurn;
import org.example.cvrag.llm.LlmGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShortMemoryService {

    private final RagProperties properties;
    private final ShortMemoryStore shortMemoryStore;
    private final LlmGateway llmGateway;

    public ShortMemoryService(RagProperties properties, ShortMemoryStore shortMemoryStore, LlmGateway llmGateway) {
        this.properties = properties;
        this.shortMemoryStore = shortMemoryStore;
        this.llmGateway = llmGateway;
    }

    public List<ConversationTurn> loadForPrompt(String userId, String sessionId) {
        List<ConversationTurn> turns = shortMemoryStore.getTurns(userId, sessionId);
        if (turns.size() <= properties.getSummaryTriggerTurns()) {
            return tail(turns, properties.getShortMemoryWindow() * 2);
        }

        int keepCount = Math.max(2, properties.getShortMemoryWindow() * 2);
        List<ConversationTurn> oldTurns = turns.subList(0, turns.size() - keepCount);
        List<ConversationTurn> latestTurns = turns.subList(turns.size() - keepCount, turns.size());

        String summary = llmGateway.summarizeTurns(oldTurns);
        List<ConversationTurn> compressed = new ArrayList<>();
        compressed.add(new ConversationTurn("system", summary, Instant.now()));
        compressed.addAll(latestTurns);
        shortMemoryStore.overwriteTurns(userId, sessionId, compressed);
        return compressed;
    }

    public void appendUserAndAssistant(String userId, String sessionId, String question, String answer) {
        shortMemoryStore.appendTurn(userId, sessionId, new ConversationTurn("user", question, Instant.now()));
        shortMemoryStore.appendTurn(userId, sessionId, new ConversationTurn("assistant", answer, Instant.now()));
    }

    public List<SessionOverview> listSessions(String userId, int limit) {
        return shortMemoryStore.listSessions(userId, limit);
    }

    public List<ConversationTurn> getSessionTurns(String userId, String sessionId) {
        return shortMemoryStore.getTurns(userId, sessionId);
    }

    public void deleteSession(String userId, String sessionId) {
        shortMemoryStore.clearSession(userId, sessionId);
    }

    private List<ConversationTurn> tail(List<ConversationTurn> turns, int maxSize) {
        if (turns.size() <= maxSize) {
            return turns;
        }
        return turns.subList(turns.size() - maxSize, turns.size());
    }
}
