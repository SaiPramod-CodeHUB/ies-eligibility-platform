package com.sgopireddy.ies.retrieval;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextSplitter")
class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter();

    @Test void emptyInputProducesNoChunks() {
        assertThat(splitter.split("")).isEmpty();
        assertThat(splitter.split(null)).isEmpty();
    }

    @Test
    @DisplayName("a short document stays as one chunk")
    void shortDocumentIsOneChunk() {
        assertThat(splitter.split("One short paragraph about resources.")).hasSize(1);
    }

    @Test
    @DisplayName("chunks carry overlap so an answer spanning a boundary survives")
    void chunksOverlap() {
        StringBuilder doc = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            doc.append("Paragraph ").append(i).append(" ")
               .append("word ".repeat(30)).append("\n\n");
        }

        List<String> chunks = splitter.split(doc.toString(), 60, 0.2);

        assertThat(chunks).hasSizeGreaterThan(1);
        // the tail of one chunk reappears at the head of the next
        boolean someOverlap = false;
        for (int i = 1; i < chunks.size(); i++) {
            String prevTail = chunks.get(i - 1);
            String head = chunks.get(i).split("\n\n")[0];
            if (prevTail.contains(head)) someOverlap = true;
        }
        assertThat(someOverlap).as("consecutive chunks share a boundary paragraph").isTrue();
    }

    @Test
    @DisplayName("an oversized single paragraph is split on sentence boundaries")
    void splitsLongParagraphOnSentences() {
        String para = "This is a sentence. ".repeat(80);
        List<String> chunks = splitter.split(para, 40, 0.15);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.strip()).endsWith("."));
    }
}
