package com.sgopireddy.ies.retrieval;

import java.util.List;

/**
 * One retrievable passage.
 *
 * Sized to hold a single coherent idea: too small and the surrounding context
 * is lost, too large and the embedding becomes an average of several topics and
 * stops discriminating. {@code source} and {@code section} exist so an answer
 * can cite where it came from, which is the whole point.
 */
public record Chunk(
        String id,
        Corpus corpus,
        String source,
        String section,
        String text,
        List<Float> embedding
) {
    public Chunk withEmbedding(List<Float> e) {
        return new Chunk(id, corpus, source, section, text, e);
    }

    public String citation() {
        return section == null || section.isBlank() ? source : source + ", " + section;
    }
}
