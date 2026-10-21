package com.sgopireddy.ies.rules;

import com.sgopireddy.ies.jurisdiction.Jurisdiction;
import com.sgopireddy.ies.thresholds.ThresholdSet;

/**
 * Everything a rule may read: the household, the jurisdiction administering the
 * program, and the thresholds in force there on the determination date.
 *
 * Passing all three in — rather than letting rules look anything up — keeps
 * them pure. Same context, same result, always.
 */
public record RuleContext(
        HouseholdSnapshot household,
        Jurisdiction jurisdiction,
        ThresholdSet thresholds
) {}
