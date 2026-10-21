package com.sgopireddy.ies.rules.food;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.rules.*;
import org.springframework.stereotype.Component;
import java.util.List;

/** Federal baseline for food assistance. Applies wherever no state variant exists. */
@Component
public class FoodAssistanceRuleSet implements ProgramRuleSet {

    private final FoodCategoricalEligibility categorical;
    private final FoodGrossIncomeTest grossIncome;
    private final FoodResourceLimitTest resources;

    public FoodAssistanceRuleSet(FoodCategoricalEligibility categorical,
                                 FoodGrossIncomeTest grossIncome,
                                 FoodResourceLimitTest resources) {
        this.categorical = categorical;
        this.grossIncome = grossIncome;
        this.resources = resources;
    }

    @Override public ProgramType programType() { return ProgramType.FOOD_ASSISTANCE; }

    @Override public List<EligibilityRule> rules() {
        return List.of(categorical, grossIncome, resources);
    }

    @Override public List<EligibilityRule> shortCircuitRules() {
        return List.of(categorical);
    }
}
