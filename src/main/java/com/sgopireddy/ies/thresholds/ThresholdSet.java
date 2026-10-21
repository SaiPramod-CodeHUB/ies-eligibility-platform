package com.sgopireddy.ies.thresholds;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * An effective-dated set of monetary thresholds, resolved for one jurisdiction.
 *
 * Thresholds change — poverty guidelines are reissued annually, resource limits
 * are adjusted by statute — and they differ between jurisdictions. A
 * determination made last quarter must still be explainable after they change,
 * so thresholds are versioned data with an effective date rather than constants
 * compiled into rules.
 *
 * {@code version} identifies both the period and the jurisdiction whose values
 * were applied, e.g. {@code 2026.1/NY}.
 */
public record ThresholdSet(
        String version,
        String jurisdictionCode,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,                 // null = currently in force
        Map<Integer, BigDecimal> monthlyFpl,
        BigDecimal fplPerAdditionalPerson,
        Map<String, BigDecimal> namedLimits
) {

    public boolean inForceOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) return false;
        return effectiveTo == null || date.isBefore(effectiveTo);
    }

    public BigDecimal fplFor(int householdSize) {
        if (householdSize <= 0) {
            throw new IllegalArgumentException("household size must be positive, was " + householdSize);
        }
        BigDecimal exact = monthlyFpl.get(householdSize);
        if (exact != null) return exact;

        int largest = monthlyFpl.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new IllegalStateException("threshold set " + version + " has no FPL table"));
        return monthlyFpl.get(largest)
                .add(fplPerAdditionalPerson.multiply(BigDecimal.valueOf(householdSize - (long) largest)));
    }

    public BigDecimal fplPercentFor(int householdSize, int percent) {
        return fplFor(householdSize)
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal limit(String name) {
        BigDecimal v = namedLimits.get(name);
        if (v == null) {
            throw new IllegalStateException(
                    "threshold set " + version + " has no limit named '" + name + "'");
        }
        return v;
    }

    /**
     * The federal baseline with this jurisdiction's overrides laid on top.
     *
     * The VERSION is deliberately unchanged: it identifies the effective-dated
     * baseline, and the jurisdiction is carried in its own field. Folding the
     * code into the version string would make two different things —
     * "which guidelines" and "whose limits" — indistinguishable in an audit.
     */
    public ThresholdSet overriddenBy(String jurisdictionCode, Map<String, BigDecimal> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return new ThresholdSet(version, jurisdictionCode,
                    effectiveFrom, effectiveTo, monthlyFpl, fplPerAdditionalPerson, namedLimits);
        }
        Map<String, BigDecimal> merged = new java.util.LinkedHashMap<>(namedLimits);
        merged.putAll(overrides);
        return new ThresholdSet(version, jurisdictionCode,
                effectiveFrom, effectiveTo, monthlyFpl, fplPerAdditionalPerson, Map.copyOf(merged));
    }
}
