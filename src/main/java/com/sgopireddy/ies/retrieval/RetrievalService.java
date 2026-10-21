package com.sgopireddy.ies.retrieval;

import com.sgopireddy.ies.ai.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The retrieval pipeline: ingest documents offline, answer questions online.
 *
 * The design constraint throughout is that an answer must be traceable to the
 * passages it came from. A caseworker acting on "the policy says X" needs to be
 * able to open the policy and see X.
 *
 * Retrieval quality and generation quality are separate problems, and they are
 * measured separately. If the right chunk is not in the candidate set, no amount
 * of prompt work recovers the answer — so {@link #retrieve} is usable on its own
 * and {@link #answer} builds on it.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    /** Retrieve wide... */
    private static final int CANDIDATE_K = 20;
    /** ...rerank narrow. */
    private static final int CONTEXT_N = 4;

    private static final String SYSTEM_PROMPT = """
            You answer questions for benefits caseworkers from the provided context.

            Rules you must follow without exception:
            1. Answer ONLY from the CONTEXT passages. Use no outside knowledge.
            2. Cite the passage number for every statement, like [1] or [2].
            3. If the context does not contain the answer, say exactly:
               "The provided policy material does not cover this."
               Do not guess, and do not fill the gap from general knowledge.
            4. Never state a dollar amount, date or threshold that is not in CONTEXT.
            5. Be brief. A caseworker is reading this between appointments.
            """;

    private final TextSplitter splitter;
    private final EmbeddingClient embeddings;
    private final VectorStore store;
    private final Reranker reranker;
    private final LlmClient llm;

    public RetrievalService(TextSplitter splitter, EmbeddingClient embeddings,
                            VectorStore store, Reranker reranker, LlmClient llm) {
        this.splitter = splitter;
        this.embeddings = embeddings;
        this.store = store;
        this.reranker = reranker;
        this.llm = llm;
    }

    // ---------------------------------------------------------------- ingest

    /**
     * Chunk, embed and index one document. Deleting by {@code source} before
     * inserting is what prevents stale chunks — the most common way a retrieval
     * system becomes quietly wrong is by answering accurately from a superseded
     * version of a document.
     */
    public int ingest(Corpus corpus, String source, String section, String text) {
        List<String> parts = splitter.split(text);
        List<Chunk> chunks = new ArrayList<>(parts.size());

        for (int i = 0; i < parts.size(); i++) {
            String body = parts.get(i);
            // The section is part of the chunk identity. Without it, two sections
            // of the same document collide on id and the second silently
            // overwrites the first.
            String id = String.join("::", corpus.name(), source, String.valueOf(section), String.valueOf(i));
            chunks.add(new Chunk(id, corpus, source, section, body, embeddings.embed(body)));
        }

        // Delete then insert. Upsert alone would leave orphaned chunks behind
        // whenever an edited document produces fewer chunks than before.
        store.deleteByDocument(corpus, source, section);
        store.upsert(chunks);
        log.info("Ingested {} chunks from {} into {} using embedding model {}",
                chunks.size(), source, corpus, embeddings.modelId());
        return chunks.size();
    }

    // --------------------------------------------------------------- retrieve

    /** Stage one and two only: candidates, then rerank. No generation. */
    public List<Scored<Chunk>> retrieve(Corpus corpus, String query) {
        return retrieve(corpus, query, CONTEXT_N);
    }

    public List<Scored<Chunk>> retrieve(Corpus corpus, String query, int topN) {
        List<Float> queryVector = embeddings.embed(query);
        List<Scored<Chunk>> candidates = store.search(corpus, queryVector, CANDIDATE_K);
        if (candidates.isEmpty()) return List.of();
        return reranker.rerank(query, candidates, topN);
    }

    // ----------------------------------------------------------------- answer

    /**
     * Retrieve, then generate an answer grounded in what was retrieved.
     *
     * When the model is unavailable the passages are still returned — a
     * caseworker with the three most relevant policy sections and no summary is
     * better served than one with an error page.
     */
    public Answer answer(Corpus corpus, String query) {
        List<Scored<Chunk>> context = retrieve(corpus, query);

        if (context.isEmpty()) {
            return new Answer("The provided policy material does not cover this.",
                    List.of(), llm.modelId(), false);
        }

        String userPrompt = buildPrompt(context, query);
        try {
            String generated = llm.complete(SYSTEM_PROMPT, userPrompt);
            return new Answer(generated, toCitations(context), llm.modelId(), true);
        } catch (RuntimeException ex) {
            log.warn("Model unavailable, degrading to passages only: {}", ex.getMessage());
            return new Answer(null, toCitations(context), llm.modelId(), false);
        }
    }

    private String buildPrompt(List<Scored<Chunk>> context, String query) {
        StringBuilder p = new StringBuilder("FACTS:\n");
        for (int i = 0; i < context.size(); i++) {
            Chunk c = context.get(i).value();
            p.append("- [%d] (%s) %s\n".formatted(i + 1, c.citation(), c.text().replace('\n', ' ')));
        }
        p.append("\nREQUEST:\n").append(query).append('\n');
        return p.toString();
    }

    private List<Citation> toCitations(List<Scored<Chunk>> context) {
        List<Citation> out = new ArrayList<>(context.size());
        for (int i = 0; i < context.size(); i++) {
            Scored<Chunk> s = context.get(i);
            out.add(new Citation(i + 1, s.value().citation(), s.value().text(), s.score()));
        }
        return out;
    }

    // ------------------------------------------------------------------ types

    /**
     * @param answer    generated text, or null when the model was unavailable
     * @param citations the passages the answer is grounded in
     * @param generated false when degraded to passages only
     */
    public record Answer(String answer, List<Citation> citations, String modelId, boolean generated) {}

    public record Citation(int index, String source, String passage, double score) {}
}
