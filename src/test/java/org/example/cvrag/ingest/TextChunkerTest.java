package org.example.cvrag.ingest;

import org.example.cvrag.config.RagProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class TextChunkerTest {

    @Test
    void shouldSplitLongText() {
        RagProperties props = new RagProperties();
        props.setChunkSize(220);
        props.setChunkOverlap(40);
        TextChunker chunker = new TextChunker(props);
        String text = "这是第一段。".repeat(120);
        List<String> chunks = chunker.chunk(text);
        Assertions.assertFalse(chunks.isEmpty());
        Assertions.assertTrue(chunks.size() >= 2);
    }
}
