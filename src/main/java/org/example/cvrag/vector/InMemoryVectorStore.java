package org.example.cvrag.vector;

import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.VectorMatch;
import org.example.cvrag.util.VectorMath;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "rag.vector", name = "provider", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, Map<String, ChunkRecord>> namespaces = new ConcurrentHashMap<>();

    @Override
    public void upsert(String namespace, ChunkRecord chunk) {
        namespaces.computeIfAbsent(namespace, key -> new ConcurrentHashMap<>()).put(chunk.id(), chunk);
    }

    @Override
    public List<VectorMatch> searchByVector(String namespace, List<Float> queryVector, int topK, Map<String, String> filter) {
        Map<String, ChunkRecord> records = namespaces.getOrDefault(namespace, Map.of());
        List<VectorMatch> matches = new ArrayList<>();
        for (ChunkRecord chunk : records.values()) {
            if (!matchedByFilter(chunk, filter)) {
                continue;
            }
            double score = VectorMath.cosine(queryVector, chunk.vector());
            matches.add(new VectorMatch(chunk, score));
        }
        matches.sort(Comparator.comparingDouble(VectorMatch::denseScore).reversed());
        return matches.stream().limit(Math.max(1, topK)).toList();
    }

    @Override
    public void deleteByDocId(String namespace, String docId) {
        Map<String, ChunkRecord> records = namespaces.get(namespace);
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> targetIds = records.values().stream()
                .filter(chunk -> docId.equals(chunk.docId()))
                .map(ChunkRecord::id)
                .toList();
        targetIds.forEach(records::remove);
    }

    private boolean matchedByFilter(ChunkRecord chunk, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();
            if ("_docId".equals(key) && !expected.equals(chunk.docId())) {
                return false;
            }
            if ("_modality".equals(key) && !expected.equalsIgnoreCase(chunk.modality().name())) {
                return false;
            }
            String actual = chunk.metadata().get(key);
            if (!"_docId".equals(key) && !"_modality".equals(key) && !expected.equals(actual)) {
                return false;
            }
        }
        return true;
    }
}
