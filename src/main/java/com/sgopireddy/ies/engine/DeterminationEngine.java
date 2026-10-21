package com.sgopireddy.ies.engine;

import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.jurisdiction.*;
import com.sgopireddy.ies.rules.*;
import com.sgopireddy.ies.thresholds.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs a program's rule set against a household snapshot, for a jurisdiction,
 * and produces a {@link Determination} carrying the full audit trail.
 *
 * Nothing here is specific to a program, a state or a city. Three abstractions
 * carry all the variation:
 *
 *   {@link ProgramRuleSet}     what the rules are, federally and per jurisdiction
 *   {@link ThresholdProvider}  what the numbers are, effective-dated per jurisdiction
 *   {@link Jurisdiction}       which programs exist and what they are called locally
 *
 * Spring injects every rule set on the classpath, so adding a program or a
 * jurisdiction means adding a class — and usually adding only configuration.
 * This class is never edited: Open/Closed, and Dependency Inversion because it
 * depends on the abstractions rather than any implementation.
 *
 * Deliberately deterministic: no AI, no external calls, no randomness. The AI
 * layers in this platform explain and retrieve; they never determine a benefit.
 */
@Service
public class DeterminationEngine {

    public static final String ENGINE_VERSION = "2026.10.1";

    private final List<ProgramRuleSet> ruleSets;
    private final ThresholdProvider thresholds;
    private final Clock clock;

    public DeterminationEngine(List<ProgramRuleSet> ruleSets,
                               ThresholdProvider thresholds,
                               Clock clock) {
        this.ruleSets = List.copyOf(ruleSets);
        this.thresholds = thresholds;
        this.clock = clock;
    }

    /**
     * Resolves the rule set for a program in a jurisdiction: most specific wins,
     * so a jurisdiction's own variant outranks the federal baseline and
     * everywhere else inherits the baseline.
     */
    public Optional<ProgramRuleSet> resolveRuleSet(Jurisdiction jurisdiction, ProgramType type) {
        return ruleSets.stream()
                .filter(rs -> rs.programType() == type)
                .filter(rs -> rs.appliesTo(jurisdiction))
                .max(Comparator.comparingInt(ProgramRuleSet::specificity));
    }

    public Determination determine(Jurisdiction jurisdiction, ProgramType type,
                                   HouseholdSnapshot household) {
        if (!jurisdiction.offers(type)) {
            throw new ProgramNotOfferedException(jurisdiction.code(), type);
        }

        ProgramRuleSet ruleSet = resolveRuleSet(jurisdiction, type)
                .orElseThrow(() -> new UnsupportedProgramException(jurisdiction.code(), type));

        // Thresholds are resolved for the jurisdiction and for the date the
        // household is assessed as of — not for today — so re-running a
        // historical determination reproduces it exactly.
        ThresholdSet inForce = thresholds.forDate(jurisdiction, household.asOf());
        RuleContext ctx = new RuleContext(household, jurisdiction, inForce);

        List<RuleResult> results = new ArrayList<>();
        for (EligibilityRule rule : ruleSet.rules()) {
            results.add(rule.evaluate(ctx));
        }

        Outcome outcome = resolveOutcome(ruleSet, results);

        List<FiredRule> trail = results.stream()
                .map(r -> new FiredRule(r.ruleId(), r.status().name(), r.reason(), r.policyReference()))
                .toList();

        return new Determination(
                household.householdId(),
                jurisdiction.code(),
                type,
                jurisdiction.nameFor(type),
                outcome,
                describeVersion(ruleSet, inForce),
                trail,
                clock.instant());
    }

    /** Every program the jurisdiction administers, in one pass. */
    public List<Determination> determineAll(Jurisdiction jurisdiction, HouseholdSnapshot household) {
        return jurisdiction.offeredPrograms().stream()
                .sorted(Comparator.comparing(ProgramType::name))
                .filter(type -> resolveRuleSet(jurisdiction, type).isPresent())
                .map(type -> determine(jurisdiction, type, household))
                .toList();
    }

    /**
     * Records what produced the decision: the engine, whether the baseline or a
     * jurisdiction variant was applied, and the threshold version. Without the
     * rule-set origin an audit cannot tell a baseline decision from an
     * overridden one.
     */
    private String describeVersion(ProgramRuleSet ruleSet, ThresholdSet thresholds) {
        String origin = ruleSet.jurisdictionCode()
                .map(c -> "rules-" + c)
                .orElse("rules-baseline");
        return String.join("/", ENGINE_VERSION, origin, "thresholds-" + thresholds.version());
    }

    /**
     * A passing short-circuit rule makes the household eligible regardless of
     * other failures (categorical eligibility). Otherwise any FAIL is
     * disqualifying, and a NEEDS_VERIFICATION with no FAIL leaves the case pending.
     */
    private Outcome resolveOutcome(ProgramRuleSet ruleSet, List<RuleResult> results) {
        Set<String> shortCircuitIds = ruleSet.shortCircuitRules().stream()
                .map(EligibilityRule::ruleId)
                .collect(Collectors.toSet());

        boolean categoricallyEligible = results.stream()
                .anyMatch(r -> shortCircuitIds.contains(r.ruleId())
                        && r.status() == RuleResult.Status.PASS);
        if (categoricallyEligible) return Outcome.ELIGIBLE;

        boolean anyFail = results.stream()
                .anyMatch(r -> r.status() == RuleResult.Status.FAIL
                        && !shortCircuitIds.contains(r.ruleId()));
        if (anyFail) return Outcome.INELIGIBLE;

        boolean anyPending = results.stream()
                .anyMatch(r -> r.status() == RuleResult.Status.NEEDS_VERIFICATION);
        return anyPending ? Outcome.PENDING_VERIFICATION : Outcome.ELIGIBLE;
    }
}
