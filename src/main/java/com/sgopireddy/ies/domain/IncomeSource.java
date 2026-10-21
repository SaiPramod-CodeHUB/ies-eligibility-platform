package com.sgopireddy.ies.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "income_source")
public class IncomeSource {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "household_id")
    private Household household;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private IncomeType type;

    /** Gross monthly amount, in whole dollars and cents. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyGross;

    private String description;

    protected IncomeSource() {}

    public IncomeSource(IncomeType type, BigDecimal monthlyGross, String description) {
        this.type = type;
        this.monthlyGross = monthlyGross;
        this.description = description;
    }

    public Long getId() { return id; }
    public IncomeType getType() { return type; }
    public BigDecimal getMonthlyGross() { return monthlyGross; }
    public String getDescription() { return description; }
    void setHousehold(Household h) { this.household = h; }

    /*
     * Identity-based equality. These entities live in a Set, and a transient
     * entity has no id yet -- using the id in equals() would make every unsaved
     * instance equal to every other, so they would collapse to one element
     * before the save. Object identity is correct here because instances are
     * never detached and re-attached in this service.
     */
    @Override public boolean equals(Object o) { return this == o; }
    @Override public int hashCode() { return System.identityHashCode(this); }
}
