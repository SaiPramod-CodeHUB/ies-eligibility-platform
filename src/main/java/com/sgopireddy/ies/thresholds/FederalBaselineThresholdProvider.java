package com.sgopireddy.ies.thresholds;

import com.sgopireddy.ies.jurisdiction.Jurisdiction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Resolves the federal baseline for a date, then lays the jurisdiction's
 * overrides on top.
 *
 * This mirrors how the programs actually work. Poverty guidelines are issued
 * federally and shared by every jurisdiction; resource limits and some income
 * disregards are set locally. So a state declares only what differs from the
 * baseline, and inherits the rest — which is why adding a jurisdiction is
 * usually configuration rather than code.
 *
 * SYNTHETIC FIGURES — illustrative, not the published HHS guidelines. In a real
 * deployment these are database rows maintained by policy staff with an
 * approval trail, not values in a class.
 */
@Component
@ConditionalOnMissingBean(name = "databaseThresholdProvider")
public class FederalBaselineThresholdProvider implements ThresholdProvider {

    // Limit names are generic. A jurisdiction maps its own programs onto them.
    public static final String FOOD_RESOURCE_LIMIT = "FOOD_RESOURCE_LIMIT";
    public static final String FOOD_RESOURCE_LIMIT_ELDERLY_DISABLED = "FOOD_RESOURCE_LIMIT_ELDERLY_DISABLED";
    public static final String CASH_RESOURCE_LIMIT = "CASH_RESOURCE_LIMIT";

    private final ThresholdOverrideProperties overrides;

    private static final List<ThresholdSet> FEDERAL = List.of(
            new ThresholdSet(
                    "2025.1", Jurisdiction.FEDERAL_CODE,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1),
                    Map.of(1, bd("1255"), 2, bd("1704"), 3, bd("2152"), 4, bd("2600"),
                           5, bd("3049"), 6, bd("3497"), 7, bd("3945"), 8, bd("4394")),
                    bd("449"),
                    Map.of(FOOD_RESOURCE_LIMIT, bd("2500"),
                           FOOD_RESOURCE_LIMIT_ELDERLY_DISABLED, bd("4000"),
                           CASH_RESOURCE_LIMIT, bd("2400"))),

            new ThresholdSet(
                    "2026.1", Jurisdiction.FEDERAL_CODE,
                    LocalDate.of(2026, 1, 1), null,          // currently in force
                    Map.of(1, bd("1305"), 2, bd("1763"), 3, bd("2221"), 4, bd("2679"),
                           5, bd("3137"), 6, bd("3595"), 7, bd("4053"), 8, bd("4511")),
                    bd("458"),
                    Map.of(FOOD_RESOURCE_LIMIT, bd("2750"),
                           FOOD_RESOURCE_LIMIT_ELDERLY_DISABLED, bd("4250"),
                           CASH_RESOURCE_LIMIT, bd("2500")))
    );

    public FederalBaselineThresholdProvider(ThresholdOverrideProperties overrides) {
        this.overrides = overrides;
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Override
    public ThresholdSet forDate(Jurisdiction jurisdiction, LocalDate date) {
        ThresholdSet baseline = FEDERAL.stream()
                .filter(s -> s.inForceOn(date))
                .findFirst()
                .orElseThrow(() -> new NoThresholdsInForceException(jurisdiction.code(), date));

        if (jurisdiction.isFederal()) return baseline;

        return baseline.overriddenBy(jurisdiction.code(),
                overrides.getOverrides().get(jurisdiction.code()));
    }
}
