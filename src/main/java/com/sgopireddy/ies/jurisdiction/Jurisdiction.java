package com.sgopireddy.ies.jurisdiction;

import com.sgopireddy.ies.domain.ProgramType;

import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * A jurisdiction that administers benefit programs: a state, a territory, a
 * county consortium, or the federal baseline itself.
 *
 * Carries what varies between them — which programs are offered, what each is
 * called locally, and the time zone a determination date is interpreted in.
 *
 * {@code FEDERAL} is the baseline a state inherits from and overrides. That
 * mirrors how these programs actually work: SNAP is federally defined with
 * state options, TANF is state-designed within federal rules, and energy
 * assistance is a federal block grant administered under state rules.
 */
public record Jurisdiction(
        String code,
        String displayName,
        ZoneId zoneId,
        Map<ProgramType, String> programNames
) {
    public static final String FEDERAL_CODE = "FEDERAL";

    public boolean isFederal() { return FEDERAL_CODE.equals(code); }

    public Set<ProgramType> offeredPrograms() { return programNames.keySet(); }

    public boolean offers(ProgramType type) { return programNames.containsKey(type); }

    /** The local name for a program, e.g. "Temporary Assistance" or "TANF". */
    public String nameFor(ProgramType type) {
        String name = programNames.get(type);
        if (name == null) {
            throw new ProgramNotOfferedException(code, type);
        }
        return name;
    }
}
