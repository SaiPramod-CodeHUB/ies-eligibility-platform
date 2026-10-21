package com.sgopireddy.ies.events;

import com.sgopireddy.ies.domain.*;
import java.time.Instant;
import java.util.List;

/**
 * Published when a determination is recorded, so downstream systems (benefit
 * issuance, notice printing, case management) react to it without this service
 * needing to know they exist.
 *
 * Carries the audit trail, not just the outcome: a consumer that prints the
 * notice needs the reasons, and asking back over HTTP would couple it to us.
 */
public record DeterminationEvent(
        String eventId,
        Long determinationId,
        Long householdId,
        String jurisdictionCode,
        ProgramType programType,
        String programName,
        Outcome outcome,
        String engineVersion,
        Instant decidedAt,
        List<Reason> reasons
) {
    public record Reason(String ruleId, String status, String reason, String policyReference) {}

    /** Partition key: all events for one household land on one partition, so they stay ordered. */
    public String partitionKey() { return String.valueOf(householdId); }

    public static DeterminationEvent from(Determination d, String eventId) {
        return new DeterminationEvent(
                eventId, d.getId(), d.getHouseholdId(), d.getJurisdictionCode(),
                d.getProgramType(), d.getProgramName(), d.getOutcome(),
                d.getRuleSetVersion(), d.getDecidedAt(),
                d.getFiredRules().stream()
                        .map(f -> new Reason(f.getRuleId(), f.getStatus(),
                                f.getReason(), f.getPolicyReference()))
                        .toList());
    }
}
