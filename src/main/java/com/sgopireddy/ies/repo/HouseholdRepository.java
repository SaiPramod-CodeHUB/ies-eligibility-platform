package com.sgopireddy.ies.repo;

import com.sgopireddy.ies.domain.Household;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

    /** Fetches members and income in one query — avoids the N+1 the rules would otherwise trigger. */
    @Query("""
           select distinct h from Household h
           left join fetch h.members
           left join fetch h.incomeSources
           where h.id = :id
           """)
    Optional<Household> findByIdWithDetail(Long id);
}
