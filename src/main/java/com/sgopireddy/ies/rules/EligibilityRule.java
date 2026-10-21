package com.sgopireddy.ies.rules;

/**
 * A single eligibility test. One rule, one concern — Single Responsibility.
 *
 * Rules are stateless and must be deterministic: no clocks, no randomness, no
 * I/O. Everything they need arrives in the {@link RuleContext}, so the same
 * context always produces the same result.
 */
public interface EligibilityRule {

    /** Stable identifier recorded on the determination audit trail. */
    String ruleId();

    RuleResult evaluate(RuleContext context);
}
