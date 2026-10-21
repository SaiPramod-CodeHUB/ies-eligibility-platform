package com.sgopireddy.ies.notices;

import com.sgopireddy.ies.ai.*;
import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.engine.EligibilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.regex.*;

/**
 * Drafts the plain-language notice for a determination.
 *
 * This is the AI boundary, and the constraint that defines it: the model is
 * handed <em>only</em> the facts the engine produced, and the output is checked
 * against those facts before it is stored. The model rephrases. It does not
 * decide, and it does not get to introduce a number.
 */
@Service
public class NoticeService {

    /** Any money-looking token in the generated text must appear in the facts. */
    private static final Pattern MONEY = Pattern.compile("\\$\\s?([0-9][0-9,]*(?:\\.[0-9]{2})?)");

    private static final String SYSTEM_PROMPT = """
            You write benefit determination notices for a state agency.

            Rules you must follow without exception:
            1. Use ONLY the facts in the FACTS block. Introduce no other information.
            2. Never state a dollar amount, date, or threshold that is not in FACTS.
            3. Do not speculate about why a household's circumstances are as they are.
            4. Plain language, short sentences, second person ("you", "your household").
            5. If FACTS does not support a statement, omit the statement.

            You are rephrasing a decision that has already been made. You are not
            making or revisiting the decision.
            """;

    private final EligibilityService eligibility;
    private final NoticeRepository notices;
    private final LlmClient llm;
    private final Clock clock;

    public NoticeService(EligibilityService eligibility, NoticeRepository notices,
                         LlmClient llm, Clock clock) {
        this.eligibility = eligibility;
        this.notices = notices;
        this.llm = llm;
        this.clock = clock;
    }

    @Transactional
    public Notice generateFor(Long determinationId) {
        Optional<Notice> existing = notices.findByDeterminationId(determinationId);
        if (existing.isPresent()) return existing.get();

        Determination d = eligibility.getDetermination(determinationId);

        String facts = buildFactsBlock(d);
        String userPrompt = facts + "\nREQUEST:\nWrite the determination notice.\n";

        String body;
        try {
            body = llm.complete(SYSTEM_PROMPT, userPrompt);
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException(
                    "Could not draft a notice for determination " + determinationId, ex);
        }

        // Grounding check: reject rather than store an indefensible notice.
        List<String> ungrounded = ungroundedFigures(body, facts);
        if (!ungrounded.isEmpty()) throw new UngroundedNoticeException(ungrounded);

        List<String> citations = d.getFiredRules().stream()
                .map(FiredRule::getPolicyReference)
                .distinct()
                .toList();

        return notices.save(new Notice(d.getId(), d.getHouseholdId(), body,
                llm.modelId(), citations, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Notice getForDetermination(Long determinationId) {
        return notices.findByDeterminationId(determinationId)
                .orElseThrow(() -> new NoticeNotFoundException(determinationId));
    }

    /**
     * The grounded facts. Nothing outside this block reaches the model, which is
     * what makes the grounding check meaningful.
     */
    private String buildFactsBlock(Determination d) {
        StringBuilder f = new StringBuilder("FACTS:\n");
        f.append("- Program: ").append(d.getProgramName()).append('\n');
        f.append("- Decision: ").append(describe(d.getOutcome())).append('\n');
        f.append("- Decision date: ").append(d.getDecidedAt()).append('\n');
        for (FiredRule r : d.getFiredRules()) {
            f.append("- ").append(r.getReason())
             .append(" (policy ").append(r.getPolicyReference()).append(")\n");
        }
        f.append("- You may appeal this decision by requesting a Fair Hearing.\n");
        return f.toString();
    }

    private String describe(Outcome outcome) {
        return switch (outcome) {
            case ELIGIBLE -> "Your household is eligible.";
            case INELIGIBLE -> "Your household is not eligible.";
            case PENDING_VERIFICATION -> "A decision is pending; further verification is needed.";
        };
    }

    /** Monetary figures present in the generated text but absent from the facts. */
    private List<String> ungroundedFigures(String generated, String facts) {
        Set<String> allowed = new HashSet<>();
        Matcher inFacts = MONEY.matcher(facts);
        while (inFacts.find()) allowed.add(normalise(inFacts.group(1)));

        List<String> ungrounded = new ArrayList<>();
        Matcher inBody = MONEY.matcher(generated);
        while (inBody.find()) {
            String value = normalise(inBody.group(1));
            if (!allowed.contains(value)) ungrounded.add("$" + inBody.group(1));
        }
        return ungrounded;
    }

    private String normalise(String money) {
        String digits = money.replace(",", "");
        if (digits.endsWith(".00")) digits = digits.substring(0, digits.length() - 3);
        return digits;
    }
}
