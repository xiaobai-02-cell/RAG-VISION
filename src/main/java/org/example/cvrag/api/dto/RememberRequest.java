package org.example.cvrag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RememberRequest(
        @NotBlank String userId,
        @NotBlank String content
) {
}
