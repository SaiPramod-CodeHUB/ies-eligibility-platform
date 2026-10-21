package com.sgopireddy.ies.engine;

public class HouseholdNotFoundException extends RuntimeException {
    public HouseholdNotFoundException(Long id) { super("Household " + id + " not found"); }
}
