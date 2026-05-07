package org.example.cvrag.llm;

import java.util.List;

public interface EmbeddingGateway {

    List<Float> embedText(String text);

    List<Float> embedTextForImageSearch(String text);

    List<Float> embedImage(byte[] imageBytes, String imageName);
}
