package com.sgopireddy.ies.rules.cash.ny;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.rules.*;
import com.sgopireddy.ies.rules.cash.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * New York's cash assistance variant.
 *
 * Reuses the baseline income and resource rules unchanged and substitutes only
 * the dependent-child rule, because that is the only place New York's policy
 * differs. Declaring {@code jurisdictionCode} makes this outrank the baseline
 * for NY and leaves every other jurisdiction untouched.
 */
@Component
public class NyCashAssistanceRuleSet implements ProgramRuleSet {

    private final CashIncomeTest income;
    private final CashResourceLimitTest resources;
    private final NySafetyNetReferralTest safetyNetReferral;

    public NyCashAssistanceRuleSet(CashIncomeTest income,
                                   CashResourceLimitTest resources,
                                   NySafetyNetReferralTest safetyNetReferral) {
        this.income = income;
        this.resources = resources;
        this.safetyNetReferral = safetyNetReferral;
    }

    @Override public ProgramType programType() { return ProgramType.CASH_ASSISTANCE; }

    @Override public Optional<String> jurisdictionCode() { return Optional.of("NY"); }

    @Override public List<EligibilityRule> rules() {
        return List.of(income, resources, safetyNetReferral);
    }
}
