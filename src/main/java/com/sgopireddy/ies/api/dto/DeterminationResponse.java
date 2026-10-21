package com.sgopireddy.ies.api.dto;

import com.sgopireddy.ies.domain.*;
import java.time.Instant;
import java.util.List;

public record DeterminationResponse(
        Long determinationId,
        Long householdId,
        String jurisdictionCode,
        ProgramType programType,
        String programName,
        Outcome outcome,
        String ruleSetVersion,
        Instant decidedAt,
        List<FiredRuleView> firedRules
) {
    public record FiredRuleView(String ruleId, String status, String reason, String policyReference) {}

    public static DeterminationResponse from(Determination d) {
        return new DeterminationResponse(
                d.getId(), d.getHouseholdId(), d.getJurisdictionCode(),
                d.getProgramType(), d.getProgramName(), d.getOutcome(),
                d.getRuleSetVersion(), d.getDecidedAt(),
                d.getFiredRules().stream()
                        .map(f -> new FiredRuleView(f.getRuleId(), f.getStatus(),
                                f.getReason(), f.getPolicyReference()))
                        .toList());
    }
}
