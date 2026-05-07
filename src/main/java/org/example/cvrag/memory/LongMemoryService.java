package org.example.cvrag.memory;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.Modality;
import org.example.cvrag.domain.VectorMatch;
import org.example.cvrag.llm.EmbeddingGateway;
import org.example.cvrag.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LongMemoryService {

    private final RagProperties properties;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStore vectorStore;

    public LongMemoryService(RagProperties properties, EmbeddingGateway embeddingGateway, VectorStore vectorStore) {
        this.properties = properties;
        this.embeddingGateway = embeddingGateway;
        this.vectorStore = vectorStore;
    }

    public String remember(String userId, String content) {
        String memoryId = "mem-" + UUID.randomUUID();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "long_memory");
        metadata.put("userId", userId);
        ChunkRecord record = new ChunkRecord(
                memoryId,
                userId,
                Modality.TEXT,
                content,
                embeddingGateway.embedText(content),
                metadata,
                Instant.now()
        );
        vectorStore.upsert(properties.getMemoryNamespace(), record);
        return memoryId;
    }

    public List<String> retrieveHints(String userId, String question, int topK) {
        List<Float> qv = embeddingGateway.embedText(question);
        Map<String, String> filter = Map.of(
                "type", "long_memory",
                "userId", userId,
                "_modality", Modality.TEXT.name()
        );
        List<VectorMatch> matches = vectorStore.searchByVector(properties.getMemoryNamespace(), qv, topK, filter);
        return matches.stream()
                .map(m -> m.chunk().content())
                .toList();
    }
}
