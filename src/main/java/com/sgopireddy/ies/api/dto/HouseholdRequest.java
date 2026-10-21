package com.sgopireddy.ies.api.dto;

import com.sgopireddy.ies.domain.IncomeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HouseholdRequest(
        /** Jurisdiction code, e.g. NY, TX, or FEDERAL. Required — see Household. */
        @NotBlank String jurisdictionCode,
        /** Local administrative area: county, parish, borough. Optional. */
        String locality,
        @NotNull @DecimalMin("0.00") BigDecimal countableResources,
        boolean heatingCostResponsible,
        @NotEmpty @Valid List<MemberRequest> members,
        @Valid List<IncomeRequest> income
) {
    public record MemberRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            boolean disabled,
            boolean receivesSsi
    ) {}

    public record IncomeRequest(
            @NotNull IncomeType type,
            @NotNull @DecimalMin("0.00") BigDecimal monthlyGross,
            String description
    ) {}
}
