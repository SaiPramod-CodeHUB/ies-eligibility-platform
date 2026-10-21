package com.sgopireddy.ies.notices;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The written notice sent to a household explaining a determination.
 *
 * Legally this is the document the household can appeal, so every statement in
 * it must be traceable to a rule the engine fired. {@code citations} records
 * those policy references, and {@code generatedBy} records which model produced
 * the wording, so a notice can be reproduced and defended later.
 */
@Entity
@Table(name = "notice")
public class Notice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long determinationId;
    @Column(nullable = false) private Long householdId;

    @Column(nullable = false, length = 8000) private String body;
    @Column(nullable = false) private String generatedBy;
    @Column(nullable = false) private Instant generatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notice_citation", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "policy_reference")
    private List<String> citations = new ArrayList<>();

    protected Notice() {}

    public Notice(Long determinationId, Long householdId, String body,
                  String generatedBy, List<String> citations, Instant generatedAt) {
        this.determinationId = determinationId;
        this.householdId = householdId;
        this.body = body;
        this.generatedBy = generatedBy;
        this.citations = new ArrayList<>(citations);
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public Long getDeterminationId() { return determinationId; }
    public Long getHouseholdId() { return householdId; }
    public String getBody() { return body; }
    public String getGeneratedBy() { return generatedBy; }
    public Instant getGeneratedAt() { return generatedAt; }
    public List<String> getCitations() { return List.copyOf(citations); }
}
