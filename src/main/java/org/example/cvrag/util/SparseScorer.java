package org.example.cvrag.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SparseScorer {

    private SparseScorer() {
    }

    public static double score(String query, String document) {
        List<String> q = TextTokenizer.tokens(query);
        List<String> d = TextTokenizer.tokens(document);
        if (q.isEmpty() || d.isEmpty()) {
            return 0.0d;
        }

        Map<String, Integer> docTf = new HashMap<>();
        for (String token : d) {
            docTf.merge(token, 1, Integer::sum);
        }

        double k1 = 1.2d;
        double b = 0.75d;
        double avgDocLength = 120.0d;
        double docLength = d.size();

        double score = 0.0d;
        for (String term : q) {
            int tf = docTf.getOrDefault(term, 0);
            if (tf == 0) {
                continue;
            }
            // Local BM25-like score. Global corpus idf is omitted in this starter.
            double idf = 1.0d;
            double numerator = tf * (k1 + 1.0d);
            double denominator = tf + k1 * (1.0d - b + b * docLength / avgDocLength);
            score += idf * (numerator / denominator);
        }
        return score;
    }
}
