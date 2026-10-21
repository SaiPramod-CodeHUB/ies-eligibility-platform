package com.sgopireddy.ies.thresholds;

import com.sgopireddy.ies.jurisdiction.Jurisdiction;
import java.time.LocalDate;

/**
 * Resolves the threshold set in force for a jurisdiction on a given date.
 *
 * Implementations resolve federal-first then apply the jurisdiction's
 * overrides: poverty guidelines are federal and shared, while resource limits
 * and some income disregards are set locally.
 */
public interface ThresholdProvider {
    ThresholdSet forDate(Jurisdiction jurisdiction, LocalDate date);
}
