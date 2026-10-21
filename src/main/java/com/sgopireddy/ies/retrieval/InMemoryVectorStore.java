package com.sgopireddy.ies.retrieval;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact (brute-force) cosine search over an in-memory index.
 *
 * Exact search is O(n) in the corpus, which is fine for a policy manual and
 * wrong for millions of chunks — that is what an approximate index like HNSW is
 * for, trading a few percent of recall for a large constant factor of speed.
 * The pgvector implementation is where that trade is made; the interface is the
 * same either way.
 */
@Component
@ConditionalOnMissingBean(name = "pgVectorStore")
public class InMemoryVectorStore implements VectorStore {

    private final Map<Corpus, Map<String, Chunk>> index = new ConcurrentHashMap<>();

    @Override
    public void upsert(List<Chunk> chunks) {
        for (Chunk c : chunks) {
            index.computeIfAbsent(c.corpus(), k -> new ConcurrentHashMap<>()).put(c.id(), c);
        }
    }

    @Override
    public void deleteByDocument(Corpus corpus, String source, String section) {
        Map<String, Chunk> corpusIndex = index.get(corpus);
        if (corpusIndex == null) return;
        corpusIndex.values().removeIf(c ->
                Objects.equals(c.source(), source) && Objects.equals(c.section(), section));
    }

    @Override
    public List<Scored<Chunk>> search(Corpus corpus, List<Float> query, int k) {
        return index.getOrDefault(corpus, Map.of()).values().stream()
                .map(c -> new Scored<>(c, cosine(query, c.embedding())))
                .sorted(Comparator.comparingDouble(Scored<Chunk>::score).reversed())
                .limit(k)
                .toList();
    }

    @Override public long count(Corpus corpus) {
        return index.getOrDefault(corpus, Map.of()).size();
    }

    @Override public void clear(Corpus corpus) {
        index.remove(corpus);
    }

    static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
