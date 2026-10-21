package com.sgopireddy.ies.rules;

import com.sgopireddy.ies.domain.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HouseholdSnapshot")
class HouseholdSnapshotTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("aggregates income by type and derives household flags")
    void buildsFromEntity() {
        var h = new Household("NY", "Albany County", new BigDecimal("800.00"), true);
        h.addMember(new HouseholdMember("Ada", "Doe", LocalDate.of(1990, 5, 1), false, false));
        h.addMember(new HouseholdMember("Kid", "Doe", LocalDate.of(2020, 3, 1), false, false));
        h.addIncome(new IncomeSource(IncomeType.EARNED, new BigDecimal("1800.00"), "wages"));
        h.addIncome(new IncomeSource(IncomeType.SELF_EMPLOYMENT, new BigDecimal("200.00"), "gig"));
        h.addIncome(new IncomeSource(IncomeType.UNEARNED, new BigDecimal("300.00"), "child support"));

        var s = HouseholdSnapshot.from(h, AS_OF);

        assertThat(s.size()).isEqualTo(2);
        assertThat(s.monthlyEarnedIncome()).isEqualByComparingTo("2000.00"); // earned + self-employment
        assertThat(s.monthlyUnearnedIncome()).isEqualByComparingTo("300.00");
        assertThat(s.monthlyGrossIncome()).isEqualByComparingTo("2300.00");
        assertThat(s.hasChildUnder18()).isTrue();
        assertThat(s.hasElderlyOrDisabledMember()).isFalse();
        assertThat(s.heatingCostResponsible()).isTrue();
    }

    @Test
    @DisplayName("a member aged 60 or over sets the elderly flag")
    void elderlyFlag() {
        var h = new Household("NY", "Albany County", BigDecimal.ZERO, false);
        h.addMember(new HouseholdMember("Eld", "Doe", LocalDate.of(1960, 1, 1), false, false));
        assertThat(HouseholdSnapshot.from(h, AS_OF).hasElderlyOrDisabledMember()).isTrue();
    }
}
