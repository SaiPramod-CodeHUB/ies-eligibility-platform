package com.sgopireddy.ies.rules.food;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;

/**
 * Categorical eligibility: a household in which a member receives SSI is
 * categorically eligible and bypasses the income and resource tests.
 * Registered as a short-circuit rule.
 */
@Component
public class FoodCategoricalEligibility implements EligibilityRule {

    private static final String POLICY_REF = "FOOD-CAT-01";

    @Override public String ruleId() { return "FOOD_CATEGORICAL_ELIGIBILITY"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        if (ctx.household().anyMemberReceivesSsi()) {
            return RuleResult.pass(ruleId(),
                    "Household is categorically eligible because a member receives SSI.",
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Household is not categorically eligible; no member receives SSI.",
                POLICY_REF);
    }
}
