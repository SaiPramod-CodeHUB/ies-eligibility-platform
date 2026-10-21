package com.sgopireddy.ies.rules.energy;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;
import java.util.List;

/** Federal baseline for energy assistance. */
@Component
public class EnergyAssistanceRuleSet implements ProgramRuleSet {

    private final EnergyIncomeTest income;
    private final EnergyHeatingResponsibilityTest cost;

    public EnergyAssistanceRuleSet(EnergyIncomeTest income, EnergyHeatingResponsibilityTest cost) {
        this.income = income; this.cost = cost;
    }

    @Override public ProgramType programType() { return ProgramType.ENERGY_ASSISTANCE; }

    @Override public List<EligibilityRule> rules() { return List.of(income, cost); }
}
