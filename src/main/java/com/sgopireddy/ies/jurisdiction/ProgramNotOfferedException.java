package com.sgopireddy.ies.jurisdiction;

import com.sgopireddy.ies.domain.ProgramType;

public class ProgramNotOfferedException extends RuntimeException {
    public ProgramNotOfferedException(String jurisdictionCode, ProgramType type) {
        super("Jurisdiction " + jurisdictionCode + " does not administer " + type);
    }
}
