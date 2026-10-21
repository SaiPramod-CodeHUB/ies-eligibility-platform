package com.sgopireddy.ies.rules;

import com.sgopireddy.ies.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable view of a household at a point in time. Rules evaluate against this,
 * never against mutable JPA entities, so a determination is reproducible: given
 * the same snapshot the engine must always produce the same outcome.
 */
public record HouseholdSnapshot(
        Long householdId,
        LocalDate asOf,
        int size,
        BigDecimal monthlyEarnedIncome,
        BigDecimal monthlyUnearnedIncome,
        BigDecimal countableResources,
        boolean hasElderlyOrDisabledMember,
        boolean anyMemberReceivesSsi,
        boolean hasChildUnder18,
        boolean heatingCostResponsible
) {

    public BigDecimal monthlyGrossIncome() {
        return monthlyEarnedIncome.add(monthlyUnearnedIncome);
    }

    public static HouseholdSnapshot from(Household h, LocalDate asOf) {
        BigDecimal earned = h.getIncomeSources().stream()
                .filter(i -> i.getType() != IncomeType.UNEARNED)
                .map(IncomeSource::getMonthlyGross)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unearned = h.getIncomeSources().stream()
                .filter(i -> i.getType() == IncomeType.UNEARNED)
                .map(IncomeSource::getMonthlyGross)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean elderlyOrDisabled = h.getMembers().stream()
                .anyMatch(m -> m.isDisabled() || m.ageAt(asOf) >= 60);

        boolean ssi = h.getMembers().stream().anyMatch(HouseholdMember::isReceivesSsi);

        boolean child = h.getMembers().stream().anyMatch(m -> m.ageAt(asOf) < 18);

        return new HouseholdSnapshot(
                h.getId(), asOf, h.getMembers().size(),
                earned, unearned, h.getCountableResources(),
                elderlyOrDisabled, ssi, child, h.isHeatingCostResponsible());
    }
}
