package com.sgopireddy.ies.jurisdiction;

import com.sgopireddy.ies.domain.ProgramType;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;

/** Resolves configured jurisdictions by code. */
@Component
public class JurisdictionRegistry {

    private final Map<String, Jurisdiction> byCode;
    private final String defaultCode;

    public JurisdictionRegistry(JurisdictionProperties props) {
        Map<String, Jurisdiction> map = new LinkedHashMap<>();

        // The federal baseline always exists, whether configured or not — a
        // jurisdiction that offers nothing of its own still inherits from it.
        map.put(Jurisdiction.FEDERAL_CODE, new Jurisdiction(
                Jurisdiction.FEDERAL_CODE, "Federal baseline", ZoneId.of("UTC"),
                Map.of(ProgramType.FOOD_ASSISTANCE, "Supplemental Nutrition Assistance Program",
                       ProgramType.CASH_ASSISTANCE, "Temporary Assistance for Needy Families",
                       ProgramType.ENERGY_ASSISTANCE, "Low Income Home Energy Assistance Program")));

        props.getJurisdictions().forEach((code, e) -> map.put(code, new Jurisdiction(
                code,
                e.getDisplayName() == null ? code : e.getDisplayName(),
                ZoneId.of(e.getZoneId()),
                Map.copyOf(e.getProgramNames()))));

        this.byCode = Collections.unmodifiableMap(map);
        this.defaultCode = props.getDefaultJurisdiction();
    }

    public Jurisdiction byCode(String code) {
        Jurisdiction j = byCode.get(code == null ? defaultCode : code);
        if (j == null) throw new UnknownJurisdictionException(code);
        return j;
    }

    public Jurisdiction federal() { return byCode.get(Jurisdiction.FEDERAL_CODE); }

    public Jurisdiction defaultJurisdiction() { return byCode(defaultCode); }

    public Collection<Jurisdiction> all() { return byCode.values(); }
}
