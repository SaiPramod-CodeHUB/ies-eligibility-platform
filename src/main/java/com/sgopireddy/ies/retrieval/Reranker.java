package com.sgopireddy.ies.retrieval;

import java.util.List;

/**
 * Reorders candidate chunks by how well each one answers the query.
 *
 * This is the step most implementations skip and where most of the retrieval
 * quality lives. Embedding search encodes the query and the chunk separately
 * and compares the two vectors — it never sees them together. A reranker scores
 * the pair, which is far more accurate and far too slow to run over a whole
 * corpus. So: retrieve wide and cheap, rerank narrow and precise.
 */
public interface Reranker {
    List<Scored<Chunk>> rerank(String query, List<Scored<Chunk>> candidates, int topN);
}
