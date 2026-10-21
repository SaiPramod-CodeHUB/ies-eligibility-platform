package com.sgopireddy.ies.rules.food;

import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Gross monthly income must not exceed 130% of the Federal Poverty Level. */
@Component
public class FoodGrossIncomeTest implements EligibilityRule {

    private static final int THRESHOLD_PERCENT = 130;
    private static final String POLICY_REF = "FOOD-GIT-130";

    @Override public String ruleId() { return "FOOD_GROSS_INCOME_TEST"; }

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
