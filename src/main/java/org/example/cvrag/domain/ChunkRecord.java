package org.example.cvrag.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ChunkRecord(
        String id,
        String docId,
        Modality modality,
        String content,
        List<Float> vector,
        Map<String, String> metadata,
        Instant createdAt
) {
}
