package com.sgopireddy.ies.engine;

import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.events.DeterminationEventPublisher;
import com.sgopireddy.ies.jurisdiction.*;
import com.sgopireddy.ies.repo.*;
import com.sgopireddy.ies.rules.HouseholdSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class EligibilityService {

    private final HouseholdRepository households;
    private final DeterminationRepository determinations;
    private final DeterminationEngine engine;
    private final DeterminationEventPublisher events;
    private final JurisdictionRegistry jurisdictions;
    private final Clock clock;

    public EligibilityService(HouseholdRepository households,
                              DeterminationRepository determinations,
                              DeterminationEngine engine,
                              DeterminationEventPublisher events,
                              JurisdictionRegistry jurisdictions,
                              Clock clock) {
        this.households = households;
        this.determinations = determinations;
        this.engine = engine;
        this.events = events;
        this.jurisdictions = jurisdictions;
        this.clock = clock;
    }

    @Transactional
    public Household createHousehold(Household household) {
        return households.save(household);
    }

    @Transactional(readOnly = true)
    public Household getHousehold(Long id) {
        return households.findByIdWithDetail(id)
                .orElseThrow(() -> new HouseholdNotFoundException(id));
    }

    /** Runs every registered program's rules, persists each determination, publishes each event. */
    @Transactional
    public List<Determination> determineAll(Long householdId) {
        Household h = load(householdId);
        Jurisdiction j = jurisdictions.byCode(h.getJurisdictionCode());
        List<Determination> saved = determinations.saveAll(
                engine.determineAll(j, snapshotOf(h, j)));
        saved.forEach(events::publish);
        return saved;
    }

    @Transactional
    public Determination determine(Long householdId, ProgramType type) {
        Household h = load(householdId);
        Jurisdiction j = jurisdictions.byCode(h.getJurisdictionCode());
        Determination saved = determinations.save(engine.determine(j, type, snapshotOf(h, j)));
        events.publish(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Determination getDetermination(Long id) {
        return determinations.findById(id)
                .orElseThrow(() -> new DeterminationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Determination> historyFor(Long householdId) {
        return determinations.findByHouseholdIdOrderByDecidedAtDesc(householdId);
    }

    private Household load(Long householdId) {
        return households.findByIdWithDetail(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    /**
     * The as-of date is resolved in the JURISDICTION's time zone, not the
     * server's. A determination run at 11pm Pacific belongs to that day in
     * California and the next day in New York, and the threshold set in force
     * can differ across that boundary.
     */
    private HouseholdSnapshot snapshotOf(Household h, Jurisdiction jurisdiction) {
        LocalDate asOf = LocalDate.ofInstant(clock.instant(), jurisdiction.zoneId());
        return HouseholdSnapshot.from(h, asOf);
    }
}
