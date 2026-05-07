package org.example.cvrag.ingest;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.Modality;
import org.example.cvrag.llm.EmbeddingGateway;
import org.example.cvrag.vector.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeIngestionService {

    private final RagProperties properties;
    private final TikaDocumentParser parser;
    private final TextChunker chunker;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStore vectorStore;

    public KnowledgeIngestionService(
            RagProperties properties,
            TikaDocumentParser parser,
            TextChunker chunker,
            EmbeddingGateway embeddingGateway,
            VectorStore vectorStore
    ) {
        this.properties = properties;
        this.parser = parser;
        this.chunker = chunker;
        this.embeddingGateway = embeddingGateway;
        this.vectorStore = vectorStore;
    }

    public IngestResult ingestText(String docId, String sourceName, String text, Map<String, String> metadata) {
        String resolvedDocId = resolveDocId(docId);
        vectorStore.deleteByDocId(properties.getKbNamespace(), resolvedDocId);
        List<String> chunks = chunker.chunk(text);
        int idx = 0;
        for (String chunk : chunks) {
            String chunkId = resolvedDocId + "-t-" + idx++;
            Map<String, String> md = new HashMap<>(metadata == null ? Map.of() : metadata);
            md.put("source", sourceName == null ? "text" : sourceName);
            md.put("chunkIndex", String.valueOf(idx));
            ChunkRecord record = new ChunkRecord(
                    chunkId,
                    resolvedDocId,
                    Modality.TEXT,
                    chunk,
                    embeddingGateway.embedText(chunk),
                    md,
                    Instant.now()
            );
            vectorStore.upsert(properties.getKbNamespace(), record);
        }
        return new IngestResult(resolvedDocId, chunks.size(), 0);
    }

    public IngestResult ingestFile(String docId, MultipartFile file, Map<String, String> metadata) {
        String content = parser.parse(file);
        return ingestText(docId, file.getOriginalFilename(), content, metadata);
    }

    public IngestResult ingestImage(String docId, MultipartFile file, String caption, Map<String, String> metadata) {
        String resolvedDocId = resolveDocId(docId);
        vectorStore.deleteByDocId(properties.getKbNamespace(), resolvedDocId);

        String effectiveText = (caption == null || caption.isBlank())
                ? "image:" + file.getOriginalFilename()
                : caption;

        try {
            Map<String, String> md = new HashMap<>(metadata == null ? Map.of() : metadata);
            md.put("source", file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
            md.put("mimeType", file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            md.put("kind", "image");
            ChunkRecord record = new ChunkRecord(
                    resolvedDocId + "-i-0",
                    resolvedDocId,
                    Modality.IMAGE,
                    effectiveText,
                    embeddingGateway.embedImage(file.getBytes(), file.getOriginalFilename()),
                    md,
                    Instant.now()
            );
            vectorStore.upsert(properties.getKbNamespace(), record);
            return new IngestResult(resolvedDocId, 0, 1);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read image bytes: " + file.getOriginalFilename(), e);
        }
    }

    public void deleteDocument(String docId) {
        vectorStore.deleteByDocId(properties.getKbNamespace(), docId);
    }

    private String resolveDocId(String docId) {
        return (docId == null || docId.isBlank()) ? "doc-" + UUID.randomUUID() : docId;
    }

    public record IngestResult(
            String docId,
            int textChunkCount,
            int imageChunkCount
    ) {
    }
}
