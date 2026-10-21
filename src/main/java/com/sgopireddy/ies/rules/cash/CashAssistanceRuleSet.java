package com.sgopireddy.ies.rules.cash;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;
import java.util.List;

/** Federal baseline for cash assistance. */
@Component
public class CashAssistanceRuleSet implements ProgramRuleSet {

    private final CashIncomeTest income;
    private final CashResourceLimitTest resources;
    private final CashDependentChildTest child;

    public CashAssistanceRuleSet(CashIncomeTest income,
                                 CashResourceLimitTest resources,
                                 CashDependentChildTest child) {
        this.income = income; this.resources = resources; this.child = child;
    }

    @Override public ProgramType programType() { return ProgramType.CASH_ASSISTANCE; }

    @Override public List<EligibilityRule> rules() {
        return List.of(income, resources, child);
    }
}
