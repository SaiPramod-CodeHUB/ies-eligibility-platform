package com.sgopireddy.ies.repo;

import com.sgopireddy.ies.domain.Determination;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeterminationRepository extends JpaRepository<Determination, Long> {
    List<Determination> findByHouseholdIdOrderByDecidedAtDesc(Long householdId);
}
