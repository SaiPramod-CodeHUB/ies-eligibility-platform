package com.sgopireddy.ies.jurisdiction;

public class UnknownJurisdictionException extends RuntimeException {
    public UnknownJurisdictionException(String code) {
        super("No jurisdiction is configured with code '" + code + "'");
    }
}
