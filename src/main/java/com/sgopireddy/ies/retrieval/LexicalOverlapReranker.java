package com.sgopireddy.ies.retrieval;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Local reranker: scores a (query, chunk) pair on query-term coverage, with a
 * mild length penalty so a long chunk cannot win on breadth alone.
 *
 * A hosted cross-encoder would be materially better. This is here so the
 * two-stage retrieve-then-rerank shape is real and tested rather than described,
 * and so its contribution is measurable — a test asserts that reranking promotes
 * the chunk that actually answers the question above one that merely shares a
 * topic with it.
 */
@Component
@ConditionalOnMissingBean(name = "hostedReranker")
public class LexicalOverlapReranker implements Reranker {

    @Override
    public List<Scored<Chunk>> rerank(String query, List<Scored<Chunk>> candidates, int topN) {
        Set<String> queryTerms = new HashSet<>(HashingEmbeddingClient.tokenise(query));
        if (queryTerms.isEmpty()) return candidates.stream().limit(topN).toList();

        return candidates.stream()
                .map(c -> new Scored<>(c.value(), pairScore(queryTerms, c)))
                .sorted(Comparator.comparingDouble(Scored<Chunk>::score).reversed())
                .limit(topN)
                .toList();
    }

    private double pairScore(Set<String> queryTerms, Scored<Chunk> candidate) {
        List<String> chunkTokens = HashingEmbeddingClient.tokenise(candidate.value().text());
        Set<String> chunkTerms = new HashSet<>(chunkTokens);

        long covered = queryTerms.stream().filter(chunkTerms::contains).count();
        double coverage = (double) covered / queryTerms.size();

        // Favour the chunk that covers the query in fewer words.
        double lengthPenalty = 1.0 / (1.0 + Math.log1p(chunkTokens.size() / 100.0));

        // Retrieval score is kept as a small tiebreaker rather than discarded.
        return (coverage * lengthPenalty * 0.9) + (candidate.score() * 0.1);
    }
}
