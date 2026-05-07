package org.example.cvrag.domain;

public record VectorMatch(
        ChunkRecord chunk,
        double denseScore
) {
}
