package org.example.cvrag.memory;

import org.example.cvrag.domain.ConversationTurn;

import java.util.List;

public interface ShortMemoryStore {

    List<ConversationTurn> getTurns(String userId, String sessionId);

    void appendTurn(String userId, String sessionId, ConversationTurn turn);

    void overwriteTurns(String userId, String sessionId, List<ConversationTurn> turns);

    List<SessionOverview> listSessions(String userId, int limit);

    void clearSession(String userId, String sessionId);
}
