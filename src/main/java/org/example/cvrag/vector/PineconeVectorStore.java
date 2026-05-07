package org.example.cvrag.vector;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.Modality;
import org.example.cvrag.domain.VectorMatch;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "rag.vector", name = "provider", havingValue = "pinecone")
public class PineconeVectorStore implements VectorStore {

    private final RestClient client;

    public PineconeVectorStore(RagProperties properties) {
        String host = properties.getPinecone().getHost();
        String apiKey = properties.getPinecone().getApiKey();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("rag.pinecone.host is required when rag.vector.provider=pinecone");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("rag.pinecone.api-key is required when rag.vector.provider=pinecone");
        }
        this.client = RestClient.builder()
                .baseUrl("https://" + host)
                .defaultHeader("Api-Key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void upsert(String namespace, ChunkRecord chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docId", chunk.docId());
        metadata.put("modality", chunk.modality().name());
        metadata.put("content", chunk.content());
        metadata.put("createdAt", chunk.createdAt().toString());
        metadata.putAll(chunk.metadata());

        Vector vector = new Vector(chunk.id(), chunk.vector(), metadata);
        UpsertRequest request = new UpsertRequest(namespace, List.of(vector));
        client.post()
                .uri("/vectors/upsert")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<VectorMatch> searchByVector(String namespace, List<Float> queryVector, int topK, Map<String, String> filter) {
        QueryRequest request = new QueryRequest(
                namespace,
                queryVector,
                Math.max(1, topK),
                true,
                false,
                toPineconeFilter(filter)
        );
        QueryResponse response;
        try {
            response = client.post()
                    .uri("/query")
                    .body(request)
                    .retrieve()
                    .body(QueryResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            if (isNamespaceNotFound(ex)) {
                return List.of();
            }
            throw ex;
        }
        if (response == null || response.matches() == null) {
            return List.of();
        }
        List<VectorMatch> matches = new ArrayList<>();
        for (QueryMatch item : response.matches()) {
            Map<String, Object> md = item.metadata() == null ? Map.of() : item.metadata();
            String docId = asString(md.get("docId"));
            String modalityRaw = asString(md.get("modality"));
            String content = asString(md.get("content"));
            Modality modality = parseModality(modalityRaw);
            Map<String, String> metadata = new HashMap<>();
            md.forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            ChunkRecord chunk = new ChunkRecord(
                    item.id(),
                    docId,
                    modality,
                    content,
                    item.values() == null ? queryVector : item.values(),
                    metadata,
                    parseInstant(asString(md.get("createdAt")))
            );
            matches.add(new VectorMatch(chunk, item.score()));
        }
        return matches;
    }

    @Override
    public void deleteByDocId(String namespace, String docId) {
        DeleteRequest request = new DeleteRequest(namespace, null, Map.of("docId", Map.of("$eq", docId)), false);
        try {
            client.post()
                    .uri("/vectors/delete")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            if (isNamespaceNotFound(ex)) {
                return;
            }
            throw ex;
        }
    }

    private Map<String, Object> toPineconeFilter(Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("_docId".equals(key)) {
                result.put("docId", Map.of("$eq", value));
            } else if ("_modality".equals(key)) {
                result.put("modality", Map.of("$eq", value));
            } else {
                result.put(key, Map.of("$eq", value));
            }
        }
        return result;
    }

    private String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Modality parseModality(String value) {
        if (value == null) {
            return Modality.TEXT;
        }
        try {
            return Modality.valueOf(value);
        } catch (Exception e) {
            return Modality.TEXT;
        }
    }

    private boolean isNamespaceNotFound(HttpClientErrorException.NotFound ex) {
        String body = ex.getResponseBodyAsString();
        return body != null && body.toLowerCase().contains("namespace not found");
    }

    private record UpsertRequest(
            String namespace,
            List<Vector> vectors
    ) {
    }

    private record Vector(
            String id,
            @JsonProperty("values") List<Float> values,
            Map<String, Object> metadata
    ) {
    }

    private record QueryRequest(
            String namespace,
            @JsonProperty("vector") List<Float> vector,
            int topK,
            boolean includeMetadata,
            boolean includeValues,
            Map<String, Object> filter
    ) {
    }

    private record QueryResponse(
            @JsonAlias("matches") List<QueryMatch> matches
    ) {
    }

    private record QueryMatch(
            String id,
            double score,
            @JsonProperty("values") List<Float> values,
            Map<String, Object> metadata
    ) {
    }

    private record DeleteRequest(
            String namespace,
            List<String> ids,
            Map<String, Object> filter,
            boolean deleteAll
    ) {
    }
}
