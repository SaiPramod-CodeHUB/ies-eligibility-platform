package com.sgopireddy.ies.rules.energy;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Energy assistance uses a more generous income ceiling: 150% of FPL. */
@Component
public class EnergyIncomeTest implements EligibilityRule {

    private static final int THRESHOLD_PERCENT = 150;
    private static final String POLICY_REF = "ENERGY-INC-150";

    @Override public String ruleId() { return "ENERGY_INCOME_TEST"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        BigDecimal limit = ctx.thresholds().fplPercentFor(ctx.household().size(), THRESHOLD_PERCENT);
        BigDecimal gross = ctx.household().monthlyGrossIncome();

        String detail = "%d%% FPL limit of $%s for a household of %d (thresholds %s)"
                .formatted(THRESHOLD_PERCENT, limit.toPlainString(),
                           ctx.household().size(), ctx.thresholds().version());

        if (gross.compareTo(limit) <= 0) {
            return RuleResult.pass(ruleId(),
                    "Gross monthly income $%s is within the %s.".formatted(gross.toPlainString(), detail),
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Gross monthly income $%s exceeds the %s.".formatted(gross.toPlainString(), detail),
                POLICY_REF);
    }
}
