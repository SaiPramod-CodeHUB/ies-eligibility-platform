package com.sgopireddy.ies.rules;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.jurisdiction.Jurisdiction;

import java.util.List;
import java.util.Optional;

/**
 * The rules for one program type, either as the federal baseline or as one
 * jurisdiction's variant.
 *
 * Resolution is most-specific-wins: a rule set naming a jurisdiction beats the
 * baseline for that jurisdiction, and everywhere else the baseline applies. So
 * onboarding a state needs Java only where that state has a rule the baseline
 * cannot express — otherwise it is configuration.
 *
 * Adding either a program or a jurisdiction means adding a class. The engine is
 * never edited: Open/Closed.
 */
public interface ProgramRuleSet {

    ProgramType programType();

    /**
     * The jurisdiction this set is specific to, or empty for the federal
     * baseline that applies wherever nothing more specific exists.
     */
    default Optional<String> jurisdictionCode() { return Optional.empty(); }

    List<EligibilityRule> rules();

    /**
     * Rules that, when they PASS, make the household eligible regardless of any
     * FAIL from the remaining rules (categorical eligibility).
     */
    default List<EligibilityRule> shortCircuitRules() { return List.of(); }

    default boolean appliesTo(Jurisdiction jurisdiction) {
        return jurisdictionCode().map(c -> c.equals(jurisdiction.code())).orElse(true);
    }

    /** Higher wins. A jurisdiction-specific set outranks the baseline. */
    default int specificity() { return jurisdictionCode().isPresent() ? 1 : 0; }
}
