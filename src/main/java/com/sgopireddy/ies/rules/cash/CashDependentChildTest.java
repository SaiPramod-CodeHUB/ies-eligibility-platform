package com.sgopireddy.ies.rules.cash;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;

/**
 * Federal baseline: cash assistance for families requires a dependent child.
 * Without one the household is ineligible under this program.
 *
 * Jurisdictions that operate a separate adult-only program override this —
 * see {@code NyCashAssistanceRuleSet}.
 */
@Component
public class CashDependentChildTest implements EligibilityRule {

    private static final String POLICY_REF = "CASH-FAM-CHILD";

    @Override public String ruleId() { return "CASH_DEPENDENT_CHILD_TEST"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        if (ctx.household().hasChildUnder18()) {
            return RuleResult.pass(ruleId(),
                    "Household includes at least one dependent child under 18.",
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Household includes no dependent child under 18, which this program requires.",
                POLICY_REF);
    }
}
