package com.sgopireddy.ies.retrieval;

import java.util.List;

/**
 * Vector search behind an interface so the in-memory implementation serves
 * tests and local runs while pgvector serves a deployment — the retrieval
 * service is written against this and does not change between them.
 */
public interface VectorStore {

    void upsert(List<Chunk> chunks);

    /**
     * Removes every chunk belonging to one document.
     *
     * Needed because upsert alone cannot prevent stale chunks: if a document is
     * edited and gets shorter, the chunks beyond the new end would survive and
     * keep being retrieved. Delete-then-insert is the only correct re-index.
     */
    void deleteByDocument(Corpus corpus, String source, String section);

    /** The k nearest chunks in one corpus, nearest first. */
    List<Scored<Chunk>> search(Corpus corpus, List<Float> queryEmbedding, int k);

    long count(Corpus corpus);

    void clear(Corpus corpus);
}
