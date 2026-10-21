package com.sgopireddy.ies.retrieval;

import org.slf4j.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds a small synthetic corpus so {@code /assist} works immediately after
 * {@code docker compose up} rather than returning nothing until someone
 * remembers to ingest.
 *
 * SYNTHETIC CONTENT — written for this demonstration. Not real policy, and not
 * real hearing decisions.
 */
@Component
@ConditionalOnProperty(name = "ies.retrieval.seed", havingValue = "true", matchIfMissing = true)
public class CorpusSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusSeeder.class);

    private final RetrievalService retrieval;

    public CorpusSeeder(RetrievalService retrieval) { this.retrieval = retrieval; }

    @Override
    public void run(ApplicationArguments args) {
        int policy = 0;
        policy += retrieval.ingest(Corpus.POLICY_MANUAL, "Policy Manual", "Section 4.2 Gross Income Test", """
                The gross income test compares a household's total countable monthly income against
                130 percent of the Federal Poverty Level for its size. Countable income includes both
                earned income, such as wages and self-employment income, and unearned income, such as
                child support, unemployment insurance and Social Security benefits.

                Self-employment income is countable. It is calculated as gross receipts less allowable
                business expenses for the same period. A household member who reports self-employment
                must provide records for the preceding twelve months, or for the period of operation
                where the business has operated for less than twelve months.

                A household that fails the gross income test is not eligible, unless it is categorically
                eligible under Section 4.6.
                """);

        policy += retrieval.ingest(Corpus.POLICY_MANUAL, "Policy Manual", "Section 4.4 Resource Limits", """
                Countable resources include cash on hand, money in checking and savings accounts, and
                other liquid assets readily convertible to cash. The standard resource limit applies to
                most households.

                A household containing a member who is age sixty or over, or who is disabled, is subject
                to a higher resource limit. The higher limit applies to the household as a whole, not
                only to the member who meets the condition.

                The home a household lives in is excluded from countable resources, as is one vehicle
                used for transportation to employment or medical care.
                """);

        policy += retrieval.ingest(Corpus.POLICY_MANUAL, "Policy Manual", "Section 4.6 Categorical Eligibility", """
                A household in which a member receives Supplemental Security Income is categorically
                eligible. A categorically eligible household is not subject to the gross income test or
                the resource limit.

                Categorical eligibility does not suppress the record of the other tests. The
                determination record must show every test that was applied and its result, so that the
                basis of the decision remains reviewable.
                """);

        policy += retrieval.ingest(Corpus.POLICY_MANUAL, "Policy Manual", "Section 7.1 Fair Hearings", """
                A household may request a Fair Hearing to appeal a determination. The request must be
                made within sixty days of the date of the notice of determination.

                The agency must produce the determination record at the hearing, including each rule
                applied, the reason recorded for that rule and the policy reference relied upon. A
                determination that cannot be explained by reference to the policy in force on the date
                of decision cannot be sustained.
                """);

        int hearings = 0;
        hearings += retrieval.ingest(Corpus.FAIR_HEARING_DECISIONS, "Decision 2025-0417", "Self-employment income", """
                The appellant contended that gross receipts from a sole proprietorship should not be
                counted as income because the business operated at a loss over the year.

                Held: gross receipts less allowable business expenses is the correct measure, and the
                agency applied it correctly for the period under review. However, the agency used a
                three-month period where the business had operated for eleven months. The determination
                is remanded for recalculation over the full period of operation.
                """);

        hearings += retrieval.ingest(Corpus.FAIR_HEARING_DECISIONS, "Decision 2025-0892", "Resource limit, disabled member", """
                The appellant's household was found ineligible on the resource limit. The household
                included an adult child receiving disability benefits.

                Held: the higher resource limit applies to the household as a whole where any member
                meets the age or disability condition. The agency applied the standard limit. The
                determination is reversed and the household is found eligible on resources.
                """);

        hearings += retrieval.ingest(Corpus.FAIR_HEARING_DECISIONS, "Decision 2026-0133", "Notice adequacy", """
                The appellant argued that the notice of determination did not state why the household
                was found ineligible, only that it was.

                Held: a notice must state the specific test the household failed and the figure it was
                compared against. A notice reciting only the outcome deprives the household of the
                ability to appeal meaningfully. The determination is vacated and the agency is directed
                to reissue an adequate notice.
                """);

        log.info("Seeded synthetic corpora: {} policy chunks, {} hearing-decision chunks",
                policy, hearings);
    }
}
