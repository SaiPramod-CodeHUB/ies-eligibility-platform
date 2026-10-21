package com.sgopireddy.ies.retrieval;

import java.util.List;

/**
 * Behind an interface for the same reason as the LLM: the retrieval pipeline is
 * testable and runnable offline.
 *
 * The same implementation must embed both documents and queries — vectors from
 * different models live in different spaces and comparing them yields nonsense
 * with no error. {@link #modelId()} is recorded on the index so a model change
 * forces a re-embed rather than silently corrupting results.
 */
public interface EmbeddingClient {

    List<Float> embed(String text);

    default List<List<Float>> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    int dimensions();

    String modelId();
}
