package com.sgopireddy.ies.domain;

/**
 * Generic benefit program types.
 *
 * Deliberately NOT named after any one state's programs. The same category of
 * benefit carries a different name in every jurisdiction — cash assistance is
 * "Temporary Assistance" in New York, "TANF" in Texas, "CalWORKs" in
 * California — and energy assistance is "HEAP" in some states and "LIHEAP"
 * elsewhere. The type is the stable concept; the name is jurisdiction data.
 */
public enum ProgramType {

    /** Food benefits. Federally defined (SNAP) with state options. */
    FOOD_ASSISTANCE,

    /** Cash assistance. State-designed within federal TANF rules. */
    CASH_ASSISTANCE,

    /** Energy and heating assistance. Federal block grant, state-administered. */
    ENERGY_ASSISTANCE
}
