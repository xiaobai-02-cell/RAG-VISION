package org.example.cvrag.api.dto;

public record IngestResponse(
        String docId,
        int textChunkCount,
        int imageChunkCount,
        String message
) {
}
