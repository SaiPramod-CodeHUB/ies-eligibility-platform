package com.sgopireddy.ies.rules.cash;

import com.sgopireddy.ies.rules.*;
import com.sgopireddy.ies.thresholds.FederalBaselineThresholdProvider;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CashResourceLimitTest implements EligibilityRule {

    private static final String POLICY_REF = "CASH-RES-01";

    @Override public String ruleId() { return "CASH_RESOURCE_LIMIT_TEST"; }

    @Override
    public RuleResult evaluate(RuleContext ctx) {
        BigDecimal limit = ctx.thresholds().limit(FederalBaselineThresholdProvider.CASH_RESOURCE_LIMIT);
        BigDecimal resources = ctx.household().countableResources();

        if (resources.compareTo(limit) <= 0) {
            return RuleResult.pass(ruleId(),
                    "Countable resources $%s are within the %s limit of $%s."
                            .formatted(resources.toPlainString(), ctx.jurisdiction().code(),
                                       limit.toPlainString()),
                    POLICY_REF);
        }
        return RuleResult.fail(ruleId(),
                "Countable resources $%s exceed the %s limit of $%s."
                        .formatted(resources.toPlainString(), ctx.jurisdiction().code(),
                                   limit.toPlainString()),
                POLICY_REF);
    }
}
