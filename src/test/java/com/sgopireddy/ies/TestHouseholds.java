package com.sgopireddy.ies;

import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.jurisdiction.Jurisdiction;
import com.sgopireddy.ies.rules.*;
import com.sgopireddy.ies.thresholds.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * Synthetic fixtures. No real benefit data is used anywhere in this project.
 *
 * The fixtures are jurisdiction-aware because the production code is: a rule is
 * evaluated against a {@link RuleContext} that carries the jurisdiction and the
 * thresholds in force for it, so a test that wants to prove a local variation
 * builds a context for that jurisdiction rather than a global one.
 */
public final class TestHouseholds {

    public static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);

    /** The baseline every jurisdiction inherits from, with no overrides. */
    public static final Jurisdiction FEDERAL = new Jurisdiction(
            Jurisdiction.FEDERAL_CODE, "Federal baseline", ZoneId.of("UTC"),
            Map.of(ProgramType.FOOD_ASSISTANCE, "Supplemental Nutrition Assistance Program",
                   ProgramType.CASH_ASSISTANCE, "Temporary Assistance for Needy Families",
                   ProgramType.ENERGY_ASSISTANCE, "Low Income Home Energy Assistance Program"));

    /** A jurisdiction with its own program names and, in tests, its own cash rule. */
    public static final Jurisdiction NY = new Jurisdiction(
            "NY", "New York State", ZoneId.of("America/New_York"),
            Map.of(ProgramType.FOOD_ASSISTANCE, "Supplemental Nutrition Assistance Program (SNAP)",
                   ProgramType.CASH_ASSISTANCE, "Temporary Assistance (TA)",
                   ProgramType.ENERGY_ASSISTANCE, "Home Energy Assistance Program (HEAP)"));

    /** A jurisdiction that deliberately does NOT offer the energy program. */
    public static final Jurisdiction TX = new Jurisdiction(
            "TX", "State of Texas", ZoneId.of("America/Chicago"),
            Map.of(ProgramType.FOOD_ASSISTANCE, "SNAP Food Benefits",
                   ProgramType.CASH_ASSISTANCE, "Temporary Assistance for Needy Families (TANF)"));

    private TestHouseholds() {}

    /** A provider with no overrides: every jurisdiction sees the federal numbers. */
    public static ThresholdProvider provider() {
        return new FederalBaselineThresholdProvider(new ThresholdOverrideProperties());
    }

    /**
     * A provider where one jurisdiction overrides named limits. This is the
     * mechanism a real deployment uses to onboard a state whose resource limits
     * differ from the baseline — configuration, not code.
     */
    public static ThresholdProvider providerWithOverrides(String code, Map<String, BigDecimal> limits) {
        ThresholdOverrideProperties props = new ThresholdOverrideProperties();
        props.getOverrides().put(code, limits);
        return new FederalBaselineThresholdProvider(props);
    }

    public static ThresholdSet thresholds() { return provider().forDate(FEDERAL, AS_OF); }

    public static ThresholdSet thresholdsOn(LocalDate date) {
        return provider().forDate(FEDERAL, date);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private long id = 1L;
        private LocalDate asOf = AS_OF;
        private int size = 3;
        private BigDecimal earned = BigDecimal.ZERO;
        private BigDecimal unearned = BigDecimal.ZERO;
        private BigDecimal resources = BigDecimal.ZERO;
        private boolean elderlyOrDisabled = false;
        private boolean ssi = false;
        private boolean childUnder18 = true;
        private boolean heating = true;
        private Jurisdiction jurisdiction = FEDERAL;
        private ThresholdProvider thresholds = provider();

        public Builder id(long v) { this.id = v; return this; }
        public Builder asOf(LocalDate v) { this.asOf = v; return this; }
        public Builder size(int v) { this.size = v; return this; }
        public Builder earned(String v) { this.earned = new BigDecimal(v); return this; }
        public Builder unearned(String v) { this.unearned = new BigDecimal(v); return this; }
        public Builder resources(String v) { this.resources = new BigDecimal(v); return this; }
        public Builder elderlyOrDisabled(boolean v) { this.elderlyOrDisabled = v; return this; }
        public Builder ssi(boolean v) { this.ssi = v; return this; }
        public Builder childUnder18(boolean v) { this.childUnder18 = v; return this; }
        public Builder heatingResponsible(boolean v) { this.heating = v; return this; }
        public Builder in(Jurisdiction v) { this.jurisdiction = v; return this; }
        public Builder withThresholds(ThresholdProvider v) { this.thresholds = v; return this; }

        public HouseholdSnapshot build() {
            return new HouseholdSnapshot(id, asOf, size, earned, unearned, resources,
                    elderlyOrDisabled, ssi, childUnder18, heating);
        }

        /** Snapshot plus the jurisdiction and the thresholds in force on its as-of date. */
        public RuleContext context() {
            HouseholdSnapshot h = build();
            return new RuleContext(h, jurisdiction, thresholds.forDate(jurisdiction, h.asOf()));
        }
    }
}
