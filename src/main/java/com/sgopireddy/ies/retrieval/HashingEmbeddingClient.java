package com.sgopireddy.ies.retrieval;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Deterministic local embedding: hashed bag-of-words projected into a fixed
 * number of dimensions, then L2-normalised so cosine similarity is a dot
 * product.
 *
 * This is NOT a semantic model — it matches on shared vocabulary, not shared
 * meaning, so "vacation" and "paid time off" are far apart where a real model
 * would put them together. It exists so the surrounding pipeline (chunking,
 * indexing, top-k, reranking, citation) is fully tested and demonstrable with
 * no API key, and so the quality contribution of the reranker is visible.
 * Swap in a hosted embedding model and nothing else changes.
 */
@Component
@ConditionalOnMissingBean(name = "hostedEmbeddingClient")
public class HashingEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 256;

    @Override
    public List<Float> embed(String text) {
        float[] v = new float[DIMENSIONS];
        for (String token : tokenise(text)) {
            int bucket = Math.abs(token.hashCode()) % DIMENSIONS;
            v[bucket] += 1.0f;
        }
        double norm = 0;
        for (float f : v) norm += f * f;
        norm = Math.sqrt(norm);

        List<Float> out = new ArrayList<>(DIMENSIONS);
        for (float f : v) out.add(norm == 0 ? 0f : (float) (f / norm));
        return List.copyOf(out);
    }

    static List<String> tokenise(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() > 2)
                .toList();
    }

    @Override public int dimensions() { return DIMENSIONS; }
    @Override public String modelId() { return "hashing-bow-256-v1"; }
}
