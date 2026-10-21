package com.sgopireddy.ies.thresholds;

import com.sgopireddy.ies.TestHouseholds;
import com.sgopireddy.ies.jurisdiction.Jurisdiction;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.sgopireddy.ies.thresholds.FederalBaselineThresholdProvider.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Effective-dated thresholds")
class ThresholdSetTest {

    private static final Jurisdiction FEDERAL = TestHouseholds.FEDERAL;
    private static final Jurisdiction NY = TestHouseholds.NY;

    private final ThresholdProvider provider = TestHouseholds.provider();

    private ThresholdSet on(LocalDate date) { return provider.forDate(FEDERAL, date); }

    @Test void resolvesTheSetInForceOnADate() {
        assertThat(on(LocalDate.of(2025, 6, 1)).version()).isEqualTo("2025.1");
        assertThat(on(LocalDate.of(2026, 6, 1)).version()).isEqualTo("2026.1");
    }

    @Test
    @DisplayName("boundary: the effectiveTo date belongs to the next set")
    void boundaryIsExclusiveAtTheEnd() {
        assertThat(on(LocalDate.of(2025, 12, 31)).version()).isEqualTo("2025.1");
        assertThat(on(LocalDate.of(2026, 1, 1)).version()).isEqualTo("2026.1");
    }

    @Test void currentSetHasNoEndDate() {
        assertThat(on(LocalDate.of(2030, 1, 1)).version()).isEqualTo("2026.1");
    }

    @Test void refusesADateBeforeAnySet() {
        assertThatThrownBy(() -> on(LocalDate.of(2000, 1, 1)))
                .isInstanceOf(NoThresholdsInForceException.class);
    }

    @Test void fplPercentRoundsToCents() {
        var t = on(LocalDate.of(2026, 6, 1));
        assertThat(t.fplPercentFor(3, 130)).isEqualByComparingTo("2887.30");
        assertThat(t.fplPercentFor(3, 150)).isEqualByComparingTo("3331.50");
    }

    @Test
    @DisplayName("extrapolates beyond the largest tabulated household size")
    void extrapolatesAboveTable() {
        var t = on(LocalDate.of(2026, 6, 1));
        assertThat(t.fplFor(10)).isEqualByComparingTo("5427"); // 4511 + 2*458
    }

    @Test void rejectsNonPositiveSize() {
        var t = on(LocalDate.of(2026, 6, 1));
        assertThatThrownBy(() -> t.fplFor(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("an unknown named limit fails loudly rather than defaulting to zero")
    void unknownLimitThrows() {
        var t = on(LocalDate.of(2026, 6, 1));
        assertThatThrownBy(() -> t.limit("NOT_A_LIMIT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOT_A_LIMIT");
    }

    // ----------------------------------------------------- jurisdiction overlay

    @Test
    @DisplayName("a jurisdiction with no overrides sees the federal numbers unchanged")
    void inheritsBaselineWhenNothingIsOverridden() {
        var baseline = provider.forDate(FEDERAL, LocalDate.of(2026, 6, 1));
        var ny = provider.forDate(NY, LocalDate.of(2026, 6, 1));

        assertThat(ny.limit(FOOD_RESOURCE_LIMIT))
                .isEqualByComparingTo(baseline.limit(FOOD_RESOURCE_LIMIT));
        assertThat(ny.monthlyFpl()).isEqualTo(baseline.monthlyFpl());
    }

    @Test
    @DisplayName("an override replaces only the limits it names, and leaves the rest "
               + "and the federal poverty guidelines alone")
    void overrideIsNarrow() {
        var withOverride = TestHouseholds.providerWithOverrides(
                "NY", Map.of(FOOD_RESOURCE_LIMIT, new BigDecimal("3000")));

        var ny = withOverride.forDate(NY, LocalDate.of(2026, 6, 1));
        var baseline = withOverride.forDate(FEDERAL, LocalDate.of(2026, 6, 1));

        assertThat(ny.limit(FOOD_RESOURCE_LIMIT)).isEqualByComparingTo("3000");
        assertThat(ny.limit(CASH_RESOURCE_LIMIT))
                .isEqualByComparingTo(baseline.limit(CASH_RESOURCE_LIMIT));
        assertThat(ny.fplPercentFor(3, 130)).isEqualByComparingTo("2887.30");
    }

    @Test
    @DisplayName("an overridden set still carries the baseline version, plus its jurisdiction, "
               + "so an audit can tell which numbers were applied")
    void overriddenSetIsAttributable() {
        var withOverride = TestHouseholds.providerWithOverrides(
                "NY", Map.of(FOOD_RESOURCE_LIMIT, new BigDecimal("3000")));

        var ny = withOverride.forDate(NY, LocalDate.of(2026, 6, 1));

        assertThat(ny.version()).isEqualTo("2026.1");
        assertThat(ny.jurisdictionCode()).isEqualTo("NY");
    }

    @Test
    @DisplayName("one jurisdiction's override does not leak into another's")
    void overridesAreIsolated() {
        var withOverride = TestHouseholds.providerWithOverrides(
                "NY", Map.of(FOOD_RESOURCE_LIMIT, new BigDecimal("3000")));

        assertThat(withOverride.forDate(TestHouseholds.TX, LocalDate.of(2026, 6, 1))
                .limit(FOOD_RESOURCE_LIMIT)).isEqualByComparingTo("2750");
    }
}
