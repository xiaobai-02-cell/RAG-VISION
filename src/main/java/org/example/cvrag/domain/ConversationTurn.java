package org.example.cvrag.domain;

import java.time.Instant;

public record ConversationTurn(
        String role,
        String content,
        Instant createdAt
) {
}
