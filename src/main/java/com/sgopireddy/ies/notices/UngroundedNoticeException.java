package com.sgopireddy.ies.notices;

import java.util.List;

/**
 * Thrown when generated notice text contains a monetary figure that does not
 * appear in the grounded facts. A notice that states a number the engine never
 * produced is indefensible at a Fair Hearing, so it is rejected rather than
 * stored.
 */
public class UngroundedNoticeException extends RuntimeException {
    private final List<String> ungroundedValues;

    public UngroundedNoticeException(List<String> ungroundedValues) {
        super("Generated notice contains figures absent from the determination: " + ungroundedValues);
        this.ungroundedValues = List.copyOf(ungroundedValues);
    }

    public List<String> getUngroundedValues() { return ungroundedValues; }
}
