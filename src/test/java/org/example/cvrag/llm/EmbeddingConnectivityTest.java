package org.example.cvrag.llm;

import org.example.cvrag.config.RagProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "rag.model.mock-enabled=false",
                "rag.memory.redis-enabled=false"
        }
)
class EmbeddingConnectivityTest {

    @Autowired
    private EmbeddingGateway embeddingGateway;

    @Autowired
    private RagProperties ragProperties;

    @Test
    void shouldCallRealEmbeddingModelAndPrintVectorInfo() {
        String apiKey = ragProperties.getModel().getApiKey();
        Assertions.assertNotNull(apiKey, "DashScope API key is null");
        Assertions.assertFalse(apiKey.isBlank(), "DashScope API key is blank");
        Assertions.assertFalse(
                embeddingGateway.getClass().getSimpleName().contains("Mock"),
                "Still using MockEmbeddingGateway, mock mode not disabled"
        );

        String text = "Embedding connectivity test for text-embedding-v3";
        List<Float> vector = Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(45),
                () -> embeddingGateway.embedText(text)
        );

        Assertions.assertNotNull(vector, "Embedding response is null");
        Assertions.assertFalse(vector.isEmpty(), "Embedding response is empty");

        // Guard against fallback vector path in DashScopeEmbeddingGateway.
        List<Float> fallbackVector = DeterministicEmbeddingSupport.vectorForText("fallback::" + text, 1024);
        Assertions.assertNotEquals(
                fallbackVector,
                vector,
                "Embedding fell back to deterministic local vector; remote embedding may not be working"
        );

        int preview = Math.min(8, vector.size());
        System.out.println("EMBEDDING_MODEL: " + ragProperties.getModel().getEmbeddingModel());
        System.out.println("EMBEDDING_DIM: " + vector.size());
        System.out.println("EMBEDDING_HEAD: " + vector.subList(0, preview));
    }
}
