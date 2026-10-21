package com.sgopireddy.ies.retrieval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Caseworker assistance. Read-only and advisory: nothing here changes a
 * determination or a benefit. It removes the repetitive part of the job —
 * finding the relevant policy paragraph, and finding how similar cases were
 * decided before.
 */
@RestController
@RequestMapping("/api/v1/assist")
@Tag(name = "Caseworker assist", description = "Retrieval over policy and prior hearing decisions, with citations")
public class CaseworkerAssistController {

    private final RetrievalService retrieval;

    public CaseworkerAssistController(RetrievalService retrieval) { this.retrieval = retrieval; }

    @GetMapping("/policy")
    @Operation(summary = "Ask a question of the policy manual",
               description = "Answers only from indexed policy, with citations, or says the material does not cover it.")
    public RetrievalService.Answer policy(@RequestParam @NotBlank String q) {
        return retrieval.answer(Corpus.POLICY_MANUAL, q);
    }

    @GetMapping("/precedent")
    @Operation(summary = "Find similar prior Fair Hearing decisions",
               description = "Returns the passages themselves rather than a summary: a caseworker "
                           + "relying on precedent needs to read the decision, not a paraphrase of it.")
    public List<RetrievalService.Citation> precedent(@RequestParam @NotBlank String q,
                                                     @RequestParam(defaultValue = "4") @Min(1) @Max(10) int limit) {
        List<Scored<Chunk>> hits = retrieval.retrieve(Corpus.FAIR_HEARING_DECISIONS, q, limit);
        return java.util.stream.IntStream.range(0, hits.size())
                .mapToObj(i -> new RetrievalService.Citation(
                        i + 1, hits.get(i).value().citation(),
                        hits.get(i).value().text(), hits.get(i).score()))
                .toList();
    }

    public record IngestRequest(@NotNull Corpus corpus, @NotBlank String source,
                                String section, @NotBlank String text) {}

    @PostMapping("/ingest")
    @Operation(summary = "Index a document",
               description = "Chunk, embed and index. Re-ingesting the same source replaces its chunks.")
    public IngestResponse ingest(@RequestBody IngestRequest req) {
        int chunks = retrieval.ingest(req.corpus(), req.source(), req.section(), req.text());
        return new IngestResponse(req.corpus(), req.source(), chunks);
    }

    public record IngestResponse(Corpus corpus, String source, int chunksIndexed) {}
}
