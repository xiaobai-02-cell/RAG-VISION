package org.example.cvrag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String userId,
        @NotBlank String sessionId,
        @NotBlank String question,
        Integer topK
) {
}
