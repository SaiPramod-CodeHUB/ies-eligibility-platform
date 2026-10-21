package com.sgopireddy.ies.engine;

public class DeterminationNotFoundException extends RuntimeException {
    public DeterminationNotFoundException(Long id) { super("Determination " + id + " not found"); }
}
