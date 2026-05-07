package org.example.cvrag.vector;

import org.example.cvrag.domain.ChunkRecord;
import org.example.cvrag.domain.VectorMatch;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void upsert(String namespace, ChunkRecord chunk);

    List<VectorMatch> searchByVector(String namespace, List<Float> queryVector, int topK, Map<String, String> filter);

    void deleteByDocId(String namespace, String docId);
}
