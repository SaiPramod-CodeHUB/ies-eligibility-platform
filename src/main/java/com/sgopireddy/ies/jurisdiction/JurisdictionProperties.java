package com.sgopireddy.ies.jurisdiction;

import com.sgopireddy.ies.domain.ProgramType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jurisdictions are configuration, not code. Onboarding a new state means
 * adding a block to {@code application.yml} — program names, time zone and
 * threshold overrides — and only writing Java if that state has a rule the
 * federal baseline cannot express.
 */
@ConfigurationProperties(prefix = "ies")
public class JurisdictionProperties {

    /** Which jurisdiction to assume when a request does not name one. */
    private String defaultJurisdiction = Jurisdiction.FEDERAL_CODE;

    private Map<String, Entry> jurisdictions = new LinkedHashMap<>();

    public static class Entry {
        private String displayName;
        private String zoneId = "UTC";
        private Map<ProgramType, String> programNames = new LinkedHashMap<>();

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String v) { this.displayName = v; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String v) { this.zoneId = v; }
        public Map<ProgramType, String> getProgramNames() { return programNames; }
        public void setProgramNames(Map<ProgramType, String> v) { this.programNames = v; }
    }

    public String getDefaultJurisdiction() { return defaultJurisdiction; }
    public void setDefaultJurisdiction(String v) { this.defaultJurisdiction = v; }
    public Map<String, Entry> getJurisdictions() { return jurisdictions; }
    public void setJurisdictions(Map<String, Entry> v) { this.jurisdictions = v; }
}
