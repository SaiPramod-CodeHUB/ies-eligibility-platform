package com.sgopireddy.ies.events;

import com.sgopireddy.ies.domain.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeterminationEvent")
class DeterminationEventTest {

    private static Determination determination() {
        return new Determination(42L, "NY", ProgramType.FOOD_ASSISTANCE,
                "Supplemental Nutrition Assistance Program (SNAP)",
                Outcome.INELIGIBLE, "2026.10.1/rules-baseline/thresholds-2026.1",
                List.of(new FiredRule("FOOD_GROSS_INCOME_TEST", "FAIL", "over the limit", "FOOD-GIT-130")),
                Instant.parse("2026-10-01T12:00:00Z"));
    }

    @Test
    @DisplayName("carries the audit trail, not just the outcome")
    void carriesReasons() {
        var e = DeterminationEvent.from(determination(), "evt-1");

        assertThat(e.outcome()).isEqualTo(Outcome.INELIGIBLE);
        assertThat(e.reasons()).hasSize(1);
        assertThat(e.reasons().get(0).policyReference()).isEqualTo("FOOD-GIT-130");
        assertThat(e.engineVersion()).contains("thresholds-2026.1");
    }

    @Test
    @DisplayName("carries the jurisdiction and the local program name, so a downstream "
               + "consumer need not re-resolve them")
    void carriesJurisdiction() {
        var e = DeterminationEvent.from(determination(), "evt-1");

        assertThat(e.jurisdictionCode()).isEqualTo("NY");
        assertThat(e.programType()).isEqualTo(ProgramType.FOOD_ASSISTANCE);
        assertThat(e.programName()).isEqualTo("Supplemental Nutrition Assistance Program (SNAP)");
    }

    @Test
    @DisplayName("partitions by household so a household's events stay ordered")
    void partitionsByHousehold() {
        assertThat(DeterminationEvent.from(determination(), "evt-1").partitionKey()).isEqualTo("42");
    }
}
