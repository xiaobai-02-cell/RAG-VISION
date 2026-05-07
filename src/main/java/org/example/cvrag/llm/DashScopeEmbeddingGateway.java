package org.example.cvrag.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.example.cvrag.config.RagProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "rag.model", name = "mock-enabled", havingValue = "false")
public class DashScopeEmbeddingGateway implements EmbeddingGateway {

    private static final int FALLBACK_DIM = 1024;

    private final RagProperties properties;
    private final RestClient restClient;

    public DashScopeEmbeddingGateway(RagProperties properties) {
        this.properties = properties;
        this.restClient = DashScopeClientSupport.create(properties);
    }

    @Override
    public List<Float> embedText(String text) {
        EmbeddingRequest req = new EmbeddingRequest(
                properties.getModel().getEmbeddingModel(),
                text == null ? "" : text
        );
        EmbeddingResponse resp = restClient.post()
                .uri("/embeddings")
                .body(req)
                .retrieve()
                .body(EmbeddingResponse.class);
        if (resp == null || resp.data() == null || resp.data().isEmpty()) {
            return DeterministicEmbeddingSupport.vectorForText("fallback::" + text, FALLBACK_DIM);
        }
        return resp.data().get(0).embedding();
    }

    @Override
    public List<Float> embedTextForImageSearch(String text) {
        // In production this should call a CLIP text embedding endpoint.
        return DeterministicEmbeddingSupport.vectorForText("clip-text::" + text, FALLBACK_DIM);
    }

    @Override
    public List<Float> embedImage(byte[] imageBytes, String imageName) {
        // In production this should call a CLIP image embedding endpoint.
        byte[] name = imageName == null ? new byte[0] : imageName.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[imageBytes.length + name.length];
        System.arraycopy(imageBytes, 0, payload, 0, imageBytes.length);
        System.arraycopy(name, 0, payload, imageBytes.length, name.length);
        return DeterministicEmbeddingSupport.vectorForBytes(payload, FALLBACK_DIM);
    }

    private record EmbeddingRequest(
            String model,
            String input
    ) {
    }

    private record EmbeddingResponse(
            List<EmbeddingData> data
    ) {
    }

    private record EmbeddingData(
            @JsonAlias("embedding") List<Float> embedding
    ) {
    }
}
