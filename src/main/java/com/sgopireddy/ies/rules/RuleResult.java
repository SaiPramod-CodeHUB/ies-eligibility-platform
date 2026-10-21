package com.sgopireddy.ies.rules;

/**
 * Outcome of a single rule. {@code policyReference} is what makes a determination
 * defensible at a Fair Hearing: every decision points back to the policy that produced it.
 */
public record RuleResult(
        String ruleId,
        Status status,
        String reason,
        String policyReference
) {
    public enum Status { PASS, FAIL, NEEDS_VERIFICATION }

    public static RuleResult pass(String ruleId, String reason, String ref) {
        return new RuleResult(ruleId, Status.PASS, reason, ref);
    }

    public static RuleResult fail(String ruleId, String reason, String ref) {
        return new RuleResult(ruleId, Status.FAIL, reason, ref);
    }

    public static RuleResult needsVerification(String ruleId, String reason, String ref) {
        return new RuleResult(ruleId, Status.NEEDS_VERIFICATION, reason, ref);
    }
}
