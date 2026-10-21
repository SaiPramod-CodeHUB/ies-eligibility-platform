package com.sgopireddy.ies.retrieval;

import com.sgopireddy.ies.ai.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Retrieval pipeline")
class RetrievalPipelineTest {

    private RetrievalService retrieval;
    private VectorStore store;

    private static final String INCOME_POLICY = """
            The gross income test compares countable monthly income against 130 percent of the
            Federal Poverty Level for the household size. Countable income includes earned income
            such as wages, and unearned income such as child support.

            Self-employment income is countable. It is gross receipts less allowable business
            expenses for the same period.
            """;

    private static final String RESOURCE_POLICY = """
            Countable resources include cash, checking and savings accounts. A household containing
            a member aged sixty or over, or disabled, is subject to a higher resource limit.

            The home the household lives in is excluded, as is one vehicle used for transport to
            employment or medical care.
            """;

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorStore();
        retrieval = new RetrievalService(new TextSplitter(), new HashingEmbeddingClient(),
                store, new LexicalOverlapReranker(), new TemplateLlmClient());
    }

    @Test
    @DisplayName("ingest chunks, embeds and indexes a document")
    void ingestIndexesChunks() {
        int chunks = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);

        assertThat(chunks).isPositive();
        assertThat(store.count(Corpus.POLICY_MANUAL)).isEqualTo(chunks);
    }

    @Test
    @DisplayName("re-ingesting the same source replaces its chunks rather than duplicating them")
    void reIngestDoesNotDuplicate() {
        int first = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);
        int second = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);

        assertThat(second).isEqualTo(first);
        assertThat(store.count(Corpus.POLICY_MANUAL)).isEqualTo(first);
    }

    @Test
    @DisplayName("REGRESSION: two sections of one document do not collide on chunk id")
    void sectionsOfTheSameDocumentDoNotCollide() {
        int income = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);
        int resources = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.4", RESOURCE_POLICY);

        // Before the fix the chunk id was corpus::source::index, so section 4.4
        // overwrote 4.2 and the count stayed at `income`.
        assertThat(store.count(Corpus.POLICY_MANUAL)).isEqualTo(income + resources);
    }

    @Test
    @DisplayName("re-indexing a shortened document leaves no orphaned chunks")
    void reIndexRemovesOrphanedChunks() {
        int before = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);
        assertThat(before).isPositive();

        int after = retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2",
                "The gross income test uses 130 percent of the Federal Poverty Level.");

        assertThat(store.count(Corpus.POLICY_MANUAL))
                .as("stale chunks from the longer version must be gone")
                .isEqualTo(after);
    }

    @Test
    @DisplayName("retrieval finds the passage that answers the question")
    void retrievesRelevantPassage() {
        retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);
        retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.4", RESOURCE_POLICY);

        var hits = retrieval.retrieve(Corpus.POLICY_MANUAL, "is self-employment income countable");

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).value().text()).containsIgnoringCase("self-employment");
    }

    @Test
    @DisplayName("corpora are isolated — a policy question cannot retrieve a hearing decision")
    void corporaAreIsolated() {
        retrieval.ingest(Corpus.POLICY_MANUAL, "Manual", "4.2", INCOME_POLICY);
        retrieval.ingest(Corpus.FAIR_HEARING_DECISIONS, "Decision 1", "income",
                "Held: the agency applied the gross income test correctly.");

        var policyHits = retrieval.retrieve(Corpus.POLICY_MANUAL, "gross income test");

        assertThat(policyHits).isNotEmpty();
        assertThat(policyHits).allSatisfy(h ->
                assertThat(h.value().corpus()).isEqualTo(Corpus.POLICY_MANUAL));
    }

    @Test
    @DisplayName("an answer carries citations back to the passages it used")
    void answerIsCited() {
        retrieval.ingest(Corpus.POLICY_MANUAL, "Policy Manual", "Section 4.4", RESOURCE_POLICY);

        var answer = retrieval.answer(Corpus.POLICY_MANUAL, "what is excluded from countable resources");

        assertThat(answer.generated()).isTrue();
        assertThat(answer.citations()).isNotEmpty();
        assertThat(answer.citations().get(0).source()).isEqualTo("Policy Manual, Section 4.4");
    }

    @Test
    @DisplayName("an empty index does not invent an answer")
    void emptyIndexAdmitsIgnorance() {
        var answer = retrieval.answer(Corpus.POLICY_MANUAL, "anything at all");

        assertThat(answer.citations()).isEmpty();
        assertThat(answer.answer()).isEqualTo("The provided policy material does not cover this.");
    }

    @Test
    @DisplayName("GRACEFUL DEGRADATION: model down still returns the passages")
    void degradesToPassagesWhenModelIsDown() {
        LlmClient broken = new LlmClient() {
            @Override public String complete(String s, String u) { throw new IllegalStateException("down"); }
            @Override public String modelId() { return "broken"; }
        };
        var degraded = new RetrievalService(new TextSplitter(), new HashingEmbeddingClient(),
                store, new LexicalOverlapReranker(), broken);
        degraded.ingest(Corpus.POLICY_MANUAL, "Manual", "4.4", RESOURCE_POLICY);

        var answer = degraded.answer(Corpus.POLICY_MANUAL, "higher resource limit");

        assertThat(answer.generated()).isFalse();
        assertThat(answer.answer()).isNull();
        assertThat(answer.citations()).isNotEmpty();   // the caseworker still gets the policy
    }
}
