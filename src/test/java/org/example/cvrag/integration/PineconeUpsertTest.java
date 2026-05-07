package org.example.cvrag.integration;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.Modality;
import org.example.cvrag.domain.VectorMatch;
import org.example.cvrag.llm.EmbeddingGateway;
import org.example.cvrag.vector.VectorStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "rag.vector.provider=pinecone",
                "rag.memory.redis-enabled=false",
                "rag.model.mock-enabled=false"
        }
)
class PineconeUpsertTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingGateway embeddingGateway;

    @Autowired
    private RagProperties ragProperties;

    @Test
    void shouldUpsertOneVectorAndQueryBack() {
        Assertions.assertTrue(
                vectorStore.getClass().getName().toLowerCase().contains("pinecone"),
                "Current VectorStore is not Pinecone implementation: " + vectorStore.getClass().getName()
        );

        String runId = UUID.randomUUID().toString().replace("-", "");
        String namespace = ragProperties.getKbNamespace() + "-it";
        String docId = "it-doc-" + runId;
        String chunkId = docId + "-t-0";
        String content = "pinecone upsert integration test " + runId;

        List<Float> vector = Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(45),
                () -> embeddingGateway.embedText(content)
        );
        Assertions.assertNotNull(vector, "Embedding vector is null");
        Assertions.assertFalse(vector.isEmpty(), "Embedding vector is empty");

        ChunkRecord chunk = new ChunkRecord(
                chunkId,
                docId,
                Modality.TEXT,
                content,
                vector,
                Map.of("source", "integration-test", "case", "pinecone-upsert"),
                Instant.now()
        );

        try {
            vectorStore.upsert(namespace, chunk);

            List<VectorMatch> matches = List.of();
            boolean found = false;
            for (int i = 0; i < 12; i++) {
                matches = vectorStore.searchByVector(namespace, vector, 5, Map.of("_docId", docId));
                found = matches.stream().anyMatch(match -> chunkId.equals(match.chunk().id()));
                if (found) {
                    break;
                }
                sleepQuietly(1000L);
            }

            Assertions.assertTrue(found, "Inserted vector not found in Pinecone query results");

            VectorMatch hit = matches.stream()
                    .filter(match -> chunkId.equals(match.chunk().id()))
                    .findFirst()
                    .orElseThrow();

            Assertions.assertEquals(docId, hit.chunk().docId(), "Returned docId mismatch");
            Assertions.assertEquals(Modality.TEXT, hit.chunk().modality(), "Returned modality mismatch");

            System.out.println("PINECONE_TEST_NAMESPACE: " + namespace);
            System.out.println("PINECONE_TEST_DOC_ID: " + docId);
            System.out.println("PINECONE_TEST_CHUNK_ID: " + chunkId);
            System.out.println("PINECONE_TEST_SCORE: " + hit.denseScore());
        } finally {
            vectorStore.deleteByDocId(namespace, docId);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Pinecone consistency", ex);
        }
    }
}
