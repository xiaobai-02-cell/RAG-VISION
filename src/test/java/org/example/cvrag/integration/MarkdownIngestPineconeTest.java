package org.example.cvrag.integration;

import org.example.cvrag.ingest.KnowledgeIngestionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
class MarkdownIngestPineconeTest {

    @Autowired
    private KnowledgeIngestionService ingestionService;

    @Test
    void shouldIngestMarkdownFileToPinecone() {
        String docId = "it-md-" + UUID.randomUUID().toString().replace("-", "");
        String markdown = """
                # CV-RAG Markdown Ingest Test
                
                这是一个用于验证 Markdown 入库链路的测试文档。
                
                - 文档解析: Apache Tika
                - 语义分块: TextChunker
                - 向量化: text-embedding-v3
                - 向量库: Pinecone
                
                该段文本包含唯一标识: %s
                """.formatted(docId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-ingest.md",
                "text/markdown",
                markdown.getBytes(StandardCharsets.UTF_8)
        );

        try {
            KnowledgeIngestionService.IngestResult result = Assertions.assertTimeoutPreemptively(
                    Duration.ofSeconds(60),
                    () -> ingestionService.ingestFile(
                            docId,
                            file,
                            Map.of("channel", "file", "docType", "project-doc", "project", "cv-rag")
                    )
            );

            Assertions.assertEquals(docId, result.docId(), "docId mismatch");
            Assertions.assertTrue(result.textChunkCount() > 0, "Markdown should produce at least one text chunk");
            Assertions.assertEquals(0, result.imageChunkCount(), "Markdown ingest should not produce image chunks");

            System.out.println("MD_INGEST_TEST_DOC_ID: " + result.docId());
            System.out.println("MD_INGEST_TEST_TEXT_CHUNKS: " + result.textChunkCount());
        } finally {
            ingestionService.deleteDocument(docId);
        }
    }
}
