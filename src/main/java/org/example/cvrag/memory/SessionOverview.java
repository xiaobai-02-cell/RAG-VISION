package org.example.cvrag.memory;

import java.time.Instant;

public record SessionOverview(
        String sessionId,
        int turnCount,
        Instant lastUpdated,
        String lastUserMessage
) {
}
