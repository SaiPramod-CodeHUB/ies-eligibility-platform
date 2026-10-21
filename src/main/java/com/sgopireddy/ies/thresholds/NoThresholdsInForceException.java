package com.sgopireddy.ies.thresholds;

import java.time.LocalDate;

public class NoThresholdsInForceException extends RuntimeException {
    public NoThresholdsInForceException(String jurisdictionCode, LocalDate date) {
        super("No threshold set is in force for " + jurisdictionCode + " on " + date
              + "; a determination cannot be made without one.");
    }
}
