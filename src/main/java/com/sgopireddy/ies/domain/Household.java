package com.sgopireddy.ies.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "household")
public class Household {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The jurisdiction administering this household's benefits. No default: a
     * determination cannot be made without knowing whose rules apply, and
     * guessing one would silently produce a decision under the wrong policy.
     */
    @Column(nullable = false) private String jurisdictionCode;

    /** Local administrative area — county, parish, borough. Free text; carries no rule logic. */
    private String locality;

    /** Countable liquid resources: savings, checking, etc. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal countableResources = BigDecimal.ZERO;

    private boolean heatingCostResponsible;

    /*
     * Sets rather than Lists, deliberately. Hibernate treats a List mapping as a
     * "bag", and two bags cannot be JOIN FETCHed in one query -- it throws
     * MultipleBagFetchException. The alternatives are to fetch them in separate
     * queries (reintroducing a round trip) or to batch-size them. Sets let a
     * single query fetch both collections, which is what the rules engine needs:
     * it reads members and income together on every determination, so an N+1
     * here would be an N+1 on the hot path.
     */
    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HouseholdMember> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IncomeSource> incomeSources = new LinkedHashSet<>();

    protected Household() {}

    public Household(String jurisdictionCode, String locality,
                     BigDecimal countableResources, boolean heatingCostResponsible) {
        this.jurisdictionCode = jurisdictionCode;
        this.locality = locality;
        this.countableResources = countableResources;
        this.heatingCostResponsible = heatingCostResponsible;
    }

    public Household addMember(HouseholdMember m) {
        members.add(m); m.setHousehold(this); return this;
    }

    public Household addIncome(IncomeSource i) {
        incomeSources.add(i); i.setHousehold(this); return this;
    }

    public Long getId() { return id; }
    public String getJurisdictionCode() { return jurisdictionCode; }
    public String getLocality() { return locality; }
    public BigDecimal getCountableResources() { return countableResources; }
    public boolean isHeatingCostResponsible() { return heatingCostResponsible; }
    public Set<HouseholdMember> getMembers() { return members; }
    public Set<IncomeSource> getIncomeSources() { return incomeSources; }
}
