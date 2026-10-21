package com.sgopireddy.ies.api.dto;

import com.sgopireddy.ies.domain.Household;
import java.math.BigDecimal;

public record HouseholdResponse(Long id, String jurisdictionCode, String locality, int size,
                                BigDecimal countableResources, boolean heatingCostResponsible) {
    public static HouseholdResponse from(Household h) {
        return new HouseholdResponse(h.getId(), h.getJurisdictionCode(), h.getLocality(),
                h.getMembers().size(), h.getCountableResources(), h.isHeatingCostResponsible());
    }
}
