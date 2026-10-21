package com.sgopireddy.ies.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "household_member")
public class HouseholdMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "household_id")
    private Household household;

    @Column(nullable = false) private String firstName;
    @Column(nullable = false) private String lastName;
    @Column(nullable = false) private LocalDate dateOfBirth;
    private boolean disabled;
    private boolean receivesSsi;

    protected HouseholdMember() {}

    public HouseholdMember(String firstName, String lastName, LocalDate dob,
                           boolean disabled, boolean receivesSsi) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dob;
        this.disabled = disabled;
        this.receivesSsi = receivesSsi;
    }

    public int ageAt(LocalDate asOf) {
        return Period.between(dateOfBirth, asOf).getYears();
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean isDisabled() { return disabled; }
    public boolean isReceivesSsi() { return receivesSsi; }
    public Household getHousehold() { return household; }
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
