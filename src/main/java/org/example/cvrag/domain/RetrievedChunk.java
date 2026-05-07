package org.example.cvrag.domain;

public record RetrievedChunk(
        ChunkRecord chunk,
        double denseScore,
        double sparseScore,
        double finalScore
) {
}
