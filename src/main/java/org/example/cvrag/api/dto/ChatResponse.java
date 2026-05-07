package org.example.cvrag.api.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        String rewrittenQuery,
        List<String> citedChunkIds,
        List<String> memoryHints
) {
}
