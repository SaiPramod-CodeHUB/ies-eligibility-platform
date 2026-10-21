package com.sgopireddy.ies.retrieval;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Splits a document into overlapping chunks, preferring structural boundaries.
 *
 * Two decisions carry the quality here:
 *
 * Size — around 150 words. Small enough that the embedding represents one idea,
 * large enough that the idea keeps its context.
 *
 * Overlap — around 15%. Without it, an answer that straddles a boundary is cut
 * in half and neither chunk retrieves well: one holds the condition, the other
 * holds the number, and the answer is destroyed by where the cut fell.
 */
@Component
public class TextSplitter {

    private static final int TARGET_WORDS = 150;
    private static final double OVERLAP_RATIO = 0.15;

    public List<String> split(String text) {
        return split(text, TARGET_WORDS, OVERLAP_RATIO);
    }

    public List<String> split(String text, int targetWords, double overlapRatio) {
        if (text == null || text.isBlank()) return List.of();

        // Paragraph boundaries first — a chunk that ends at a paragraph break is
        // worth more than one 40 words longer that ends mid-sentence.
        List<String> paragraphs = Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::strip)
                .filter(p -> !p.isBlank())
                .toList();

        List<String> chunks = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int bufferWords = 0;
        int overlapWords = Math.max(1, (int) Math.round(targetWords * overlapRatio));

        for (String paragraph : paragraphs) {
            int words = wordCount(paragraph);

            if (bufferWords > 0 && bufferWords + words > targetWords) {
                chunks.add(String.join("\n\n", buffer));
                buffer = carryOverlap(buffer, overlapWords);
                bufferWords = buffer.stream().mapToInt(TextSplitter::wordCount).sum();
            }

            if (words > targetWords * 2) {
                // A single oversized paragraph is split on sentence boundaries.
                chunks.addAll(splitLongParagraph(paragraph, targetWords));
                continue;
            }

            buffer.add(paragraph);
            bufferWords += words;
        }

        if (!buffer.isEmpty()) chunks.add(String.join("\n\n", buffer));
        return List.copyOf(chunks);
    }

    private List<String> carryOverlap(List<String> buffer, int overlapWords) {
        List<String> carried = new ArrayList<>();
        int count = 0;
        for (int i = buffer.size() - 1; i >= 0 && count < overlapWords; i--) {
            carried.add(0, buffer.get(i));
            count += wordCount(buffer.get(i));
        }
        return carried;
    }

    private List<String> splitLongParagraph(String paragraph, int targetWords) {
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int words = 0;
        for (String sentence : sentences) {
            int w = wordCount(sentence);
            if (words > 0 && words + w > targetWords) {
                out.add(current.toString().strip());
                current.setLength(0);
                words = 0;
            }
            current.append(sentence).append(' ');
            words += w;
        }
        if (!current.isEmpty()) out.add(current.toString().strip());
        return out;
    }

    static int wordCount(String s) {
        if (s == null || s.isBlank()) return 0;
        return s.strip().split("\\s+").length;
    }
}
