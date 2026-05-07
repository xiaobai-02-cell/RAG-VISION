package org.example.cvrag.llm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

final class DeterministicEmbeddingSupport {

    private DeterministicEmbeddingSupport() {
    }

    static List<Float> vectorForText(String text, int dim) {
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        return vectorForBytes(bytes, dim);
    }

    static List<Float> vectorForBytes(byte[] bytes, int dim) {
        byte[] digest = sha256(bytes);
        List<Float> vector = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            int b = digest[i % digest.length] & 0xff;
            float val = (b / 255.0f) * 2.0f - 1.0f;
            vector.add(val);
        }
        return vector;
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
