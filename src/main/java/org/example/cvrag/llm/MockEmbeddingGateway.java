package org.example.cvrag.llm;

import org.example.cvrag.config.RagProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "rag.model", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockEmbeddingGateway implements EmbeddingGateway {

    private static final int TEXT_DIM = 1024;
    private static final int IMAGE_DIM = 1024;

    @SuppressWarnings("unused")
    private final RagProperties properties;

    public MockEmbeddingGateway(RagProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Float> embedText(String text) {
        return DeterministicEmbeddingSupport.vectorForText(text, TEXT_DIM);
    }

    @Override
    public List<Float> embedTextForImageSearch(String text) {
        return DeterministicEmbeddingSupport.vectorForText("clip-text::" + text, IMAGE_DIM);
    }

    @Override
    public List<Float> embedImage(byte[] imageBytes, String imageName) {
        byte[] nameBytes = imageName == null ? new byte[0] : imageName.getBytes(StandardCharsets.UTF_8);
        byte[] merged = new byte[imageBytes.length + nameBytes.length];
        System.arraycopy(imageBytes, 0, merged, 0, imageBytes.length);
        System.arraycopy(nameBytes, 0, merged, imageBytes.length, nameBytes.length);
        return DeterministicEmbeddingSupport.vectorForBytes(merged, IMAGE_DIM);
    }
}
