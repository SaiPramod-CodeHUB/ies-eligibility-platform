package com.sgopireddy.ies.rules.energy;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;

/** The household must be responsible for its own heating or cooling cost. */
@Component
public class EnergyHeatingResponsibilityTest implements EligibilityRule {

    private static final String POLICY_REF = "ENERGY-COST-01";

    @Override public String ruleId() { return "ENERGY_COST_RESPONSIBILITY_TEST"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        if (ctx.household().heatingCostResponsible()) {
            return RuleResult.pass(ruleId(),
                    "Household is directly responsible for its home energy cost.",
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Household is not directly responsible for its home energy cost.",
                POLICY_REF);
    }
}
