package com.sgopireddy.ies.rules.cash.ny;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;

/**
 * New York variant of the dependent-child test.
 *
 * The federal baseline fails a household with no dependent child, because
 * family cash assistance requires one. New York additionally operates Safety
 * Net Assistance for households that do not qualify for Family Assistance, so
 * the correct outcome is a referral for separate review — NEEDS_VERIFICATION —
 * not a denial.
 *
 * This class exists to demonstrate the override mechanism with a real example
 * of how jurisdictions genuinely differ. It is the only Java required to
 * onboard New York; everything else about NY is configuration.
 */
@Component
public class NySafetyNetReferralTest implements EligibilityRule {

    private static final String POLICY_REF = "NY-TA-SNA-REF";

    @Override public String ruleId() { return "CASH_DEPENDENT_CHILD_TEST"; }  // replaces the baseline rule

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        if (ctx.household().hasChildUnder18()) {
            return RuleResult.pass(ruleId(),
                    "Household includes at least one dependent child under 18, "
                            + "so Family Assistance criteria are met.",
                    POLICY_REF);
        }
        return RuleResult.needsVerification(ruleId(),
                "No dependent child under 18. Household may qualify under Safety Net "
                        + "Assistance rather than Family Assistance; referred for review.",
                POLICY_REF);
    }
}
