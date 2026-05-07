package org.example.cvrag.api.dto;

import org.example.cvrag.domain.ConversationTurn;

import java.util.List;

public record SessionTurnsResponse(
        String userId,
        String sessionId,
        List<ConversationTurn> turns
) {
}
