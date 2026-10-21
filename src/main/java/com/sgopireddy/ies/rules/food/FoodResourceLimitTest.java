package com.sgopireddy.ies.rules.food;

import com.sgopireddy.ies.rules.*;
import com.sgopireddy.ies.thresholds.FederalBaselineThresholdProvider;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Countable resources must not exceed the limit. Households containing an
 * elderly or disabled member have a higher limit.
 *
 * The limit itself comes from the jurisdiction's threshold set, so a state with
 * a more generous limit needs configuration rather than a different rule.
 */
@Component
public class FoodResourceLimitTest implements EligibilityRule {

    private static final String POLICY_REF = "FOOD-RES-01";

    @Override public String ruleId() { return "FOOD_RESOURCE_LIMIT_TEST"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        boolean higher = ctx.household().hasElderlyOrDisabledMember();
        BigDecimal limit = ctx.thresholds().limit(higher
                ? FederalBaselineThresholdProvider.FOOD_RESOURCE_LIMIT_ELDERLY_DISABLED
                : FederalBaselineThresholdProvider.FOOD_RESOURCE_LIMIT);
        String basis = higher
                ? "the household includes an elderly or disabled member"
                : "the standard resource limit applies";
        BigDecimal resources = ctx.household().countableResources();

        if (resources.compareTo(limit) <= 0) {
            return RuleResult.pass(ruleId(),
                    "Countable resources $%s are within the %s limit of $%s, because %s."
                            .formatted(resources.toPlainString(), ctx.jurisdiction().code(),
                                       limit.toPlainString(), basis),
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Countable resources $%s exceed the %s limit of $%s, where %s."
                        .formatted(resources.toPlainString(), ctx.jurisdiction().code(),
                                   limit.toPlainString(), basis),
                POLICY_REF);
    }
}
