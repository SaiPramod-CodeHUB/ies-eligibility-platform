package com.sgopireddy.ies.engine;

import com.sgopireddy.ies.domain.ProgramType;

public class UnsupportedProgramException extends RuntimeException {
    public UnsupportedProgramException(String jurisdictionCode, ProgramType type) {
        super("No rule set is registered for " + type + " in jurisdiction " + jurisdictionCode
              + ", and no federal baseline applies.");
    }
}
