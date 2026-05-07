package org.example.cvrag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TextIngestRequest(
        String docId,
        String sourceName,
        @NotBlank String content,
        String docType,
        String project,
        String tags
) {
}
