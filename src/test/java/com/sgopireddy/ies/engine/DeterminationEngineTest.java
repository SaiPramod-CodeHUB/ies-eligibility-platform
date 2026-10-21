package com.sgopireddy.ies.engine;

import com.sgopireddy.ies.TestHouseholds;
import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.jurisdiction.*;
import com.sgopireddy.ies.rules.ProgramRuleSet;
import com.sgopireddy.ies.rules.cash.*;
import com.sgopireddy.ies.rules.cash.ny.*;
import com.sgopireddy.ies.rules.energy.*;
import com.sgopireddy.ies.rules.food.*;
import com.sgopireddy.ies.thresholds.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;

import static com.sgopireddy.ies.thresholds.FederalBaselineThresholdProvider.FOOD_RESOURCE_LIMIT;
import static org.assertj.core.api.Assertions.*;

@DisplayName("DeterminationEngine")
class DeterminationEngineTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    private static final Jurisdiction FEDERAL = TestHouseholds.FEDERAL;
    private static final Jurisdiction NY = TestHouseholds.NY;
    private static final Jurisdiction TX = TestHouseholds.TX;

    private DeterminationEngine engine;

    /** The federal baseline rule sets: no jurisdiction code, so they apply everywhere. */
    private static List<ProgramRuleSet> baselineRuleSets() {
        var food = new FoodAssistanceRuleSet(new FoodCategoricalEligibility(),
                new FoodGrossIncomeTest(), new FoodResourceLimitTest());
        var cash = new CashAssistanceRuleSet(new CashIncomeTest(),
                new CashResourceLimitTest(), new CashDependentChildTest());
        var energy = new EnergyAssistanceRuleSet(new EnergyIncomeTest(),
                new EnergyHeatingResponsibilityTest());
        return List.of(food, cash, energy);
    }

    @BeforeEach
    void setUp() {
        engine = new DeterminationEngine(baselineRuleSets(), TestHouseholds.provider(), FIXED);
    }

    @Test
    @DisplayName("eligible household gets ELIGIBLE with every rule recorded")
    void eligibleHousehold() {
        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("500.00").build();

        Determination d = engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h);

        assertThat(d.getOutcome()).isEqualTo(Outcome.ELIGIBLE);
        assertThat(d.getFiredRules()).hasSize(3);
        assertThat(d.getDecidedAt()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
    }

    @Test
    @DisplayName("the determination records the jurisdiction and its local program name")
    void recordsJurisdictionAndLocalName() {
        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("500.00").build();

        Determination d = engine.determine(NY, ProgramType.FOOD_ASSISTANCE, h);

        assertThat(d.getJurisdictionCode()).isEqualTo("NY");
        assertThat(d.getProgramType()).isEqualTo(ProgramType.FOOD_ASSISTANCE);
        assertThat(d.getProgramName()).isEqualTo("Supplemental Nutrition Assistance Program (SNAP)");
    }

    @Test
    @DisplayName("the recorded version pins the engine, the rule-set origin and the thresholds")
    void versionPinsEngineOriginAndThresholds() {
        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("500.00").build();
        assertThat(engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h).getRuleSetVersion())
                .isEqualTo(DeterminationEngine.ENGINE_VERSION + "/rules-baseline/thresholds-2026.1");
    }

    @Test
    @DisplayName("a failing rule makes the household INELIGIBLE")
    void overIncomeIsIneligible() {
        var h = TestHouseholds.builder().size(3).earned("5000.00").build();
        assertThat(engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.INELIGIBLE);
    }

    @Test
    @DisplayName("categorical eligibility overrides failures but does not hide them")
    void categoricalShortCircuitsFailures() {
        var h = TestHouseholds.builder().size(3)
                .earned("9999.00").resources("9999.00").ssi(true).build();

        Determination d = engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h);

        assertThat(d.getOutcome()).isEqualTo(Outcome.ELIGIBLE);
        assertThat(d.getFiredRules())
                .anyMatch(f -> f.getRuleId().equals("FOOD_GROSS_INCOME_TEST")
                        && f.getStatus().equals("FAIL"));
    }

    @Test
    @DisplayName("determineAll runs every program the jurisdiction offers, exactly once")
    void determinesEveryOfferedProgram() {
        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("500.00").build();

        List<Determination> all = engine.determineAll(FEDERAL, h);

        assertThat(all).hasSize(3);
        assertThat(all).extracting(Determination::getProgramType)
                .containsExactlyInAnyOrder(ProgramType.FOOD_ASSISTANCE,
                        ProgramType.CASH_ASSISTANCE, ProgramType.ENERGY_ASSISTANCE);
    }

    @Test
    @DisplayName("determineAll skips a program the jurisdiction does not administer")
    void determineAllRespectsWhatTheJurisdictionOffers() {
        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("500.00").build();

        // Texas is configured without an energy program.
        List<Determination> all = engine.determineAll(TX, h);

        assertThat(all).extracting(Determination::getProgramType)
                .containsExactlyInAnyOrder(ProgramType.FOOD_ASSISTANCE, ProgramType.CASH_ASSISTANCE)
                .doesNotContain(ProgramType.ENERGY_ASSISTANCE);
    }

    @Test
    @DisplayName("asking for a program the jurisdiction does not offer is refused, not inherited")
    void programNotOfferedIsRefused() {
        var h = TestHouseholds.builder().build();
        assertThatThrownBy(() -> engine.determine(TX, ProgramType.ENERGY_ASSISTANCE, h))
                .isInstanceOf(ProgramNotOfferedException.class)
                .hasMessageContaining("TX");
    }

    @Test
    @DisplayName("the energy 150% ceiling admits a household the food 130% test rejects")
    void programThresholdsDiffer() {
        var h = TestHouseholds.builder().size(3).earned("3000.00").resources("100.00").build();

        assertThat(engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.INELIGIBLE);
        assertThat(engine.determine(FEDERAL, ProgramType.ENERGY_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.ELIGIBLE);
    }

    // ------------------------------------------------- jurisdiction variation

    @Test
    @DisplayName("MOST SPECIFIC WINS: a jurisdiction's rule set overrides the baseline "
               + "for that jurisdiction and nowhere else")
    void jurisdictionRuleSetOverridesBaseline() {
        var withNy = new DeterminationEngine(
                concat(baselineRuleSets(), new NyCashAssistanceRuleSet(
                        new CashIncomeTest(), new CashResourceLimitTest(),
                        new NySafetyNetReferralTest())),
                TestHouseholds.provider(), FIXED);

        // A childless household: the federal cash rule FAILs it outright, while
        // New York refers it to Safety Net and leaves the case pending.
        var h = TestHouseholds.builder().size(2)
                .earned("1000.00").resources("100.00").childUnder18(false).build();

        assertThat(withNy.determine(FEDERAL, ProgramType.CASH_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.INELIGIBLE);
        assertThat(withNy.determine(NY, ProgramType.CASH_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.PENDING_VERIFICATION);

        // Texas has no variant registered, so it inherits the baseline.
        assertThat(withNy.determine(TX, ProgramType.CASH_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.INELIGIBLE);
    }

    @Test
    @DisplayName("the audit trail says which rule set decided, so a baseline decision "
               + "cannot be mistaken for an overridden one")
    void versionRecordsTheRuleSetOrigin() {
        var withNy = new DeterminationEngine(
                concat(baselineRuleSets(), new NyCashAssistanceRuleSet(
                        new CashIncomeTest(), new CashResourceLimitTest(),
                        new NySafetyNetReferralTest())),
                TestHouseholds.provider(), FIXED);
        var h = TestHouseholds.builder().size(2).earned("1000.00").resources("100.00").build();

        assertThat(withNy.determine(NY, ProgramType.CASH_ASSISTANCE, h).getRuleSetVersion())
                .contains("rules-NY");
        assertThat(withNy.determine(TX, ProgramType.CASH_ASSISTANCE, h).getRuleSetVersion())
                .contains("rules-baseline");
    }

    @Test
    @DisplayName("CONFIGURATION ONLY: a threshold override changes the outcome "
               + "with no change to any rule class")
    void thresholdOverrideChangesTheOutcome() {
        var withOverride = new DeterminationEngine(baselineRuleSets(),
                TestHouseholds.providerWithOverrides("NY",
                        Map.of(FOOD_RESOURCE_LIMIT, new BigDecimal("3000"))),
                FIXED);

        var h = TestHouseholds.builder().size(3).earned("1500.00").resources("2900.00").build();

        assertThat(withOverride.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.INELIGIBLE);   // baseline limit 2750
        assertThat(withOverride.determine(NY, ProgramType.FOOD_ASSISTANCE, h).getOutcome())
                .isEqualTo(Outcome.ELIGIBLE);     // NY limit 3000
    }

    @Test
    @DisplayName("the as-of date is resolved per jurisdiction, so the threshold set "
               + "in force can differ across a midnight boundary")
    void asOfDateIsJurisdictionLocal() {
        // 2025-12-31 19:00 Eastern is 2026-01-01 00:00 UTC: the 2026 set is in
        // force federally and the 2025 set is still in force in New York.
        var provider = TestHouseholds.provider();
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(provider.forDate(FEDERAL,
                LocalDate.ofInstant(instant, FEDERAL.zoneId())).version()).isEqualTo("2026.1");
        assertThat(provider.forDate(NY,
                LocalDate.ofInstant(instant, NY.zoneId())).version()).isEqualTo("2025.1");
    }

    // ------------------------------------------------------------- invariants

    @Test
    @DisplayName("DETERMINISM: the same snapshot always produces the same outcome and trail")
    void isDeterministic() {
        var h = TestHouseholds.builder().size(4).earned("2600.00").resources("1200.00").build();

        Determination first = engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h);
        Determination second = engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h);

        assertThat(second.getOutcome()).isEqualTo(first.getOutcome());
        assertThat(second.getRuleSetVersion()).isEqualTo(first.getRuleSetVersion());
        assertThat(second.getFiredRules())
                .extracting(FiredRule::getRuleId, FiredRule::getStatus, FiredRule::getReason)
                .isEqualTo(first.getFiredRules().stream()
                        .map(f -> org.assertj.core.groups.Tuple.tuple(
                                f.getRuleId(), f.getStatus(), f.getReason()))
                        .toList());
    }

    @Test
    @DisplayName("REPRODUCIBILITY: a 2025 household is re-decided under 2025 thresholds")
    void historicalDeterminationUsesHistoricalThresholds() {
        var h = TestHouseholds.builder().size(3).earned("2850.00").resources("100.00")
                .asOf(LocalDate.of(2025, 6, 1)).build();

        Determination d = engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h);

        assertThat(d.getRuleSetVersion()).endsWith("thresholds-2025.1");
        assertThat(d.getOutcome()).isEqualTo(Outcome.INELIGIBLE); // over the 2025 limit of 2797.60
    }

    @Test
    @DisplayName("a program with no rule set anywhere is rejected rather than silently skipped")
    void unregisteredProgramThrows() {
        var bare = new DeterminationEngine(List.of(), TestHouseholds.provider(), FIXED);
        assertThatThrownBy(() -> bare.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE,
                TestHouseholds.builder().build()))
                .isInstanceOf(UnsupportedProgramException.class)
                .hasMessageContaining("FOOD_ASSISTANCE");
    }

    @Test
    @DisplayName("a date with no threshold set in force is refused rather than guessed")
    void noThresholdsInForce() {
        var h = TestHouseholds.builder().asOf(LocalDate.of(2019, 1, 1)).build();
        assertThatThrownBy(() -> engine.determine(FEDERAL, ProgramType.FOOD_ASSISTANCE, h))
                .isInstanceOf(NoThresholdsInForceException.class);
    }

    private static List<ProgramRuleSet> concat(List<ProgramRuleSet> base, ProgramRuleSet extra) {
        return java.util.stream.Stream.concat(base.stream(), java.util.stream.Stream.of(extra)).toList();
    }
}
