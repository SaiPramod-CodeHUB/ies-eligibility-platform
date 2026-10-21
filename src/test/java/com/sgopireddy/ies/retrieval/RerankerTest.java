package com.sgopireddy.ies.retrieval;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reranker is where most retrieval quality comes from, so its contribution
 * is asserted rather than assumed: it must promote the passage that answers the
 * question above one that merely shares a topic with it.
 */
@DisplayName("LexicalOverlapReranker")
class RerankerTest {

    private final Reranker reranker = new LexicalOverlapReranker();

    private static Scored<Chunk> chunk(String id, String text, double retrievalScore) {
        return new Scored<>(new Chunk(id, Corpus.POLICY_MANUAL, "Manual", "s", text, List.of()), retrievalScore);
    }

    @Test
    @DisplayName("promotes the passage that answers the query over a merely related one")
    void promotesTheAnsweringPassage() {
        var onTopicButUseless = chunk("a",
                "Income is important in benefits administration. Income is reviewed by staff. "
                + "Income documentation is retained. Income policy is set centrally.", 0.92);

        var actualAnswer = chunk("b",
                "Self-employment income is countable and is calculated as gross receipts "
                + "less allowable business expenses.", 0.61);

        // retrieval put the useless one first
        var candidates = List.of(onTopicButUseless, actualAnswer);

        var reranked = reranker.rerank("is self-employment income countable", candidates, 2);

        assertThat(reranked.get(0).value().id())
                .as("reranking should surface the passage that answers the question")
                .isEqualTo("b");
    }

    @Test
    @DisplayName("limits the result to topN")
    void limitsToTopN() {
        var candidates = List.of(
                chunk("a", "resource limit cash savings", 0.9),
                chunk("b", "resource limit vehicle excluded", 0.8),
                chunk("c", "income test wages", 0.7));

        assertThat(reranker.rerank("resource limit", candidates, 2)).hasSize(2);
    }

    @Test
    @DisplayName("an empty query falls back to retrieval order rather than failing")
    void emptyQueryFallsBack() {
        var candidates = List.of(chunk("a", "text here", 0.9), chunk("b", "other text", 0.5));
        assertThat(reranker.rerank("", candidates, 1)).hasSize(1);
    }
}
