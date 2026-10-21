package com.sgopireddy.ies.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of running one program's rules against one household in one
 * jurisdiction.
 *
 * {@code firedRules} is the audit trail, and it is why this engine is
 * deterministic rather than generative: at a Fair Hearing the agency must show
 * exactly which rules produced this outcome and why, under the policy in force
 * in that jurisdiction on that date.
 */
@Entity
@Table(name = "determination")
public class Determination {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long householdId;

    @Column(nullable = false) private String jurisdictionCode;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProgramType programType;

    /** The jurisdiction's own name for the program, captured at decision time. */
    @Column(nullable = false) private String programName;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Outcome outcome;

    @Column(nullable = false) private Instant decidedAt;

    /** Engine version, rule-set origin and threshold version that produced this. */
    @Column(nullable = false) private String ruleSetVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "determination_fired_rule",
            joinColumns = @JoinColumn(name = "determination_id"))
    private List<FiredRule> firedRules = new ArrayList<>();

    protected Determination() {}

    public Determination(Long householdId, String jurisdictionCode,
                         ProgramType programType, String programName, Outcome outcome,
                         String ruleSetVersion, List<FiredRule> firedRules, Instant decidedAt) {
        this.householdId = householdId;
        this.jurisdictionCode = jurisdictionCode;
        this.programType = programType;
        this.programName = programName;
        this.outcome = outcome;
        this.ruleSetVersion = ruleSetVersion;
        this.firedRules = new ArrayList<>(firedRules);
        this.decidedAt = decidedAt;
    }

    public Long getId() { return id; }
    public Long getHouseholdId() { return householdId; }
    public String getJurisdictionCode() { return jurisdictionCode; }
    public ProgramType getProgramType() { return programType; }
    public String getProgramName() { return programName; }
    public Outcome getOutcome() { return outcome; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getRuleSetVersion() { return ruleSetVersion; }
    public List<FiredRule> getFiredRules() { return List.copyOf(firedRules); }
}
