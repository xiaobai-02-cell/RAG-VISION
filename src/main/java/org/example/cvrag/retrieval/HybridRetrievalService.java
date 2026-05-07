package org.example.cvrag.retrieval;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.Modality;
import org.example.cvrag.domain.RetrievedChunk;
import org.example.cvrag.domain.VectorMatch;
import org.example.cvrag.llm.EmbeddingGateway;
import org.example.cvrag.util.SparseScorer;
import org.example.cvrag.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HybridRetrievalService {

    private final RagProperties properties;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStore vectorStore;

    public HybridRetrievalService(RagProperties properties, EmbeddingGateway embeddingGateway, VectorStore vectorStore) {
        this.properties = properties;
        this.embeddingGateway = embeddingGateway;
        this.vectorStore = vectorStore;
    }

    public List<RetrievedChunk> retrieve(String rewrittenQuery, Integer topKOverride) {
        int topK = topKOverride == null ? properties.getRetrieveTopK() : Math.max(1, topKOverride);
        int candidateK = Math.max(topK * properties.getRetrievalCandidateMultiplier(), topK);
        double alpha = properties.getHybridAlpha();

        List<Float> textQueryVector = embeddingGateway.embedText(rewrittenQuery);
        List<VectorMatch> textCandidates = vectorStore.searchByVector(
                properties.getKbNamespace(),
                textQueryVector,
                candidateK,
                Map.of("_modality", Modality.TEXT.name())
        );

        List<Float> imageQueryVector = embeddingGateway.embedTextForImageSearch(rewrittenQuery);
        List<VectorMatch> imageCandidates = vectorStore.searchByVector(
                properties.getKbNamespace(),
                imageQueryVector,
                candidateK,
                Map.of("_modality", Modality.IMAGE.name())
        );

        Map<String, CandidateScore> merged = new HashMap<>();
        merge(merged, textCandidates);
        merge(merged, imageCandidates);

        normalizeDense(merged);
        normalizeSparse(rewrittenQuery, merged);

        List<RetrievedChunk> result = new ArrayList<>();
        for (CandidateScore cs : merged.values()) {
            double finalScore = alpha * cs.denseNorm + (1.0d - alpha) * cs.sparseNorm;
            result.add(new RetrievedChunk(cs.match.chunk(), cs.match.denseScore(), cs.sparseRaw, finalScore));
        }
        result.sort(Comparator.comparingDouble(RetrievedChunk::finalScore).reversed());
        return result.stream().limit(topK).toList();
    }

    private void merge(Map<String, CandidateScore> merged, List<VectorMatch> candidates) {
        for (VectorMatch match : candidates) {
            merged.compute(match.chunk().id(), (key, old) -> {
                if (old == null || match.denseScore() > old.match.denseScore()) {
                    return new CandidateScore(match);
                }
                return old;
            });
        }
    }

    private void normalizeDense(Map<String, CandidateScore> merged) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (CandidateScore cs : merged.values()) {
            min = Math.min(min, cs.match.denseScore());
            max = Math.max(max, cs.match.denseScore());
        }
        double delta = max - min;
        for (CandidateScore cs : merged.values()) {
            cs.denseNorm = delta < 1e-9 ? 1.0d : (cs.match.denseScore() - min) / delta;
        }
    }

    private void normalizeSparse(String query, Map<String, CandidateScore> merged) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (CandidateScore cs : merged.values()) {
            cs.sparseRaw = SparseScorer.score(query, sparseDocument(cs));
            min = Math.min(min, cs.sparseRaw);
            max = Math.max(max, cs.sparseRaw);
        }
        double delta = max - min;
        for (CandidateScore cs : merged.values()) {
            cs.sparseNorm = delta < 1e-9 ? 0.0d : (cs.sparseRaw - min) / delta;
        }
    }

    private String sparseDocument(CandidateScore cs) {
        String content = cs.match.chunk().content();
        Map<String, String> metadata = cs.match.chunk().metadata();
        if (metadata == null || metadata.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder(content.length() + 96);
        sb.append(content);
        if (metadata.containsKey("docType")) {
            sb.append(" docType:").append(metadata.get("docType"));
        }
        if (metadata.containsKey("project")) {
            sb.append(" project:").append(metadata.get("project"));
        }
        if (metadata.containsKey("tags")) {
            sb.append(" tags:").append(metadata.get("tags"));
        }
        return sb.toString();
    }

    private static class CandidateScore {
        private final VectorMatch match;
        private double denseNorm;
        private double sparseRaw;
        private double sparseNorm;

        private CandidateScore(VectorMatch match) {
            this.match = match;
        }
    }
}
