package org.example.cvrag.ingest;

import org.example.cvrag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final RagProperties properties;

    public TextChunker(RagProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int chunkSize = Math.max(200, properties.getChunkSize());
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize / 2));
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + chunkSize);
            int safeEnd = findBoundary(normalized, start, end);
            String chunk = normalized.substring(start, safeEnd).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (safeEnd >= normalized.length()) {
                break;
            }
            start = Math.max(safeEnd - overlap, start + 1);
        }
        return chunks;
    }

    private int findBoundary(String text, int start, int end) {
        if (end >= text.length()) {
            return text.length();
        }
        for (int i = end; i > start + Math.max(40, properties.getChunkSize() / 3); i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '.' || c == '！' || c == '!' || c == '?' || c == '？' || c == ';' || c == '；') {
                return i;
            }
        }
        return end;
    }
}
