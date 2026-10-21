package com.sgopireddy.ies.rules;

import com.sgopireddy.ies.TestHouseholds;
import com.sgopireddy.ies.rules.food.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.sgopireddy.ies.thresholds.FederalBaselineThresholdProvider.FOOD_RESOURCE_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Food assistance rules (federal baseline)")
class FoodRulesTest {

    private final FoodGrossIncomeTest grossIncome = new FoodGrossIncomeTest();
    private final FoodResourceLimitTest resources = new FoodResourceLimitTest();
    private final FoodCategoricalEligibility categorical = new FoodCategoricalEligibility();

    @Test
    @DisplayName("gross income within 130% FPL passes, and the reason quotes the limit")
    void grossIncomeWithinLimit() {
        // 2026 baseline, size 3 -> FPL 2221, 130% = 2887.30
        var ctx = TestHouseholds.builder().size(3).earned("2500.00").context();
        var r = grossIncome.evaluate(ctx);

        assertThat(r.status()).isEqualTo(RuleResult.Status.PASS);
        assertThat(r.policyReference()).isEqualTo("FOOD-GIT-130");
        assertThat(r.reason()).contains("2887.30").contains("2026.1");
    }

    @Test
    @DisplayName("gross income above 130% FPL fails")
    void grossIncomeOverLimit() {
        var ctx = TestHouseholds.builder().size(3).earned("3000.00").context();
        assertThat(grossIncome.evaluate(ctx).status()).isEqualTo(RuleResult.Status.FAIL);
    }

    @Test
    @DisplayName("earned and unearned income both count toward the gross test")
    void bothIncomeTypesCount() {
        var ctx = TestHouseholds.builder().size(3).earned("2000.00").unearned("1000.00").context();
        assertThat(grossIncome.evaluate(ctx).status()).isEqualTo(RuleResult.Status.FAIL);
    }

    @Test
    @DisplayName("boundary: income exactly at the limit passes")
    void boundaryAtLimitPasses() {
        var ctx = TestHouseholds.builder().size(3).earned("2887.30").context();
        assertThat(grossIncome.evaluate(ctx).status()).isEqualTo(RuleResult.Status.PASS);
    }

    @Test
    @DisplayName("EFFECTIVE DATING: the same income decides differently under 2025 thresholds")
    void thresholdsAreEffectiveDated() {
        // 2025 size 3 -> FPL 2152, 130% = 2797.60 ; 2026 -> 2887.30
        String income = "2850.00";
        var under2026 = TestHouseholds.builder().size(3).earned(income)
                .asOf(LocalDate.of(2026, 6, 1)).context();
        var under2025 = TestHouseholds.builder().size(3).earned(income)
                .asOf(LocalDate.of(2025, 6, 1)).context();

        assertThat(grossIncome.evaluate(under2026).status()).isEqualTo(RuleResult.Status.PASS);
        assertThat(grossIncome.evaluate(under2025).status()).isEqualTo(RuleResult.Status.FAIL);
        assertThat(grossIncome.evaluate(under2025).reason()).contains("2025.1");
    }

    @Test
    @DisplayName("resource limit is higher when a member is elderly or disabled")
    void higherResourceLimitForElderlyOrDisabled() {
        var standard = TestHouseholds.builder().resources("3000.00").elderlyOrDisabled(false).context();
        var higher   = TestHouseholds.builder().resources("3000.00").elderlyOrDisabled(true).context();

        assertThat(resources.evaluate(standard).status()).isEqualTo(RuleResult.Status.FAIL);
        assertThat(resources.evaluate(higher).status()).isEqualTo(RuleResult.Status.PASS);
        assertThat(resources.evaluate(higher).reason()).contains("elderly or disabled");
    }

    @Test
    @DisplayName("JURISDICTION OVERRIDE: the same rule and household decide differently "
               + "when a jurisdiction raises the resource limit")
    void jurisdictionOverrideChangesTheOutcome() {
        // Baseline FOOD_RESOURCE_LIMIT is 2750 in 2026; NY raises it to 3000.
        // Same rule class, same household — only configuration differs.
        var underBaseline = TestHouseholds.builder()
                .resources("2900.00").in(TestHouseholds.FEDERAL).context();
        var underNy = TestHouseholds.builder()
                .resources("2900.00").in(TestHouseholds.NY)
                .withThresholds(TestHouseholds.providerWithOverrides(
                        "NY", Map.of(FOOD_RESOURCE_LIMIT, new BigDecimal("3000"))))
                .context();

        assertThat(resources.evaluate(underBaseline).status()).isEqualTo(RuleResult.Status.FAIL);
        assertThat(resources.evaluate(underNy).status()).isEqualTo(RuleResult.Status.PASS);
    }

    @Test
    @DisplayName("categorical eligibility passes when a member receives SSI")
    void categoricalWithSsi() {
        assertThat(categorical.evaluate(TestHouseholds.builder().ssi(true).context()).status())
                .isEqualTo(RuleResult.Status.PASS);
    }

    @Test
    @DisplayName("every rule records a policy reference for the audit trail")
    void allRulesCitePolicy() {
        var ctx = TestHouseholds.builder().context();
        assertThat(grossIncome.evaluate(ctx).policyReference()).isNotBlank();
        assertThat(resources.evaluate(ctx).policyReference()).isNotBlank();
        assertThat(categorical.evaluate(ctx).policyReference()).isNotBlank();
    }
}
