package org.example.cvrag.util;

import java.util.List;

public final class VectorMath {

    private VectorMath() {
    }

    public static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0d;
        }
        int dim = Math.min(a.size(), b.size());
        double dot = 0.0d;
        double aNorm = 0.0d;
        double bNorm = 0.0d;
        for (int i = 0; i < dim; i++) {
            double av = a.get(i);
            double bv = b.get(i);
            dot += av * bv;
            aNorm += av * av;
            bNorm += bv * bv;
        }
        if (aNorm == 0.0d || bNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(aNorm) * Math.sqrt(bNorm));
    }
}
