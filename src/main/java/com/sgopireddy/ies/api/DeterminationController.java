package com.sgopireddy.ies.api;

import com.sgopireddy.ies.api.dto.DeterminationResponse;
import com.sgopireddy.ies.domain.ProgramType;
import com.sgopireddy.ies.engine.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Determinations", description = "Deterministic, auditable eligibility determinations")
public class DeterminationController {

    private final EligibilityService service;

    public DeterminationController(EligibilityService service) { this.service = service; }

    @PostMapping("/households/{householdId}/determine")
    @Operation(summary = "Determine eligibility across every program the jurisdiction offers",
               description = "Resolves the household's jurisdiction, runs each program's rule set "
                           + "once — jurisdiction variant where one exists, federal baseline "
                           + "otherwise — and records the full audit trail.")
    public List<DeterminationResponse> determineAll(@PathVariable Long householdId) {
        return service.determineAll(householdId).stream().map(DeterminationResponse::from).toList();
    }

    @PostMapping("/households/{householdId}/determine/{programType}")
    @Operation(summary = "Determine eligibility for one program type")
    public DeterminationResponse determineOne(@PathVariable Long householdId,
                                              @PathVariable ProgramType programType) {
        return DeterminationResponse.from(service.determine(householdId, programType));
    }

    @GetMapping("/determinations/{id}")
    @Operation(summary = "Fetch a determination with every rule that fired")
    public DeterminationResponse get(@PathVariable Long id) {
        return DeterminationResponse.from(service.getDetermination(id));
    }
}
