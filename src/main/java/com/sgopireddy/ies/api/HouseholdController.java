package com.sgopireddy.ies.api;

import com.sgopireddy.ies.api.dto.*;
import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.engine.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/households")
@Tag(name = "Households", description = "Household intake")
public class HouseholdController {

    private final EligibilityService service;

    public HouseholdController(EligibilityService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a household")
    public ResponseEntity<HouseholdResponse> create(@Valid @RequestBody HouseholdRequest req,
                                                    UriComponentsBuilder uri) {
        Household h = new Household(req.jurisdictionCode(), req.locality(),
                req.countableResources(), req.heatingCostResponsible());

        req.members().forEach(m -> h.addMember(new HouseholdMember(
                m.firstName(), m.lastName(), m.dateOfBirth(), m.disabled(), m.receivesSsi())));

        if (req.income() != null) {
            req.income().forEach(i -> h.addIncome(
                    new IncomeSource(i.type(), i.monthlyGross(), i.description())));
        }

        Household saved = service.createHousehold(h);
        return ResponseEntity
                .created(uri.path("/api/v1/households/{id}").buildAndExpand(saved.getId()).toUri())
                .body(HouseholdResponse.from(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a household")
    public HouseholdResponse get(@PathVariable Long id) {
        return HouseholdResponse.from(service.getHousehold(id));
    }

    @GetMapping("/{id}/determinations")
    @Operation(summary = "Determination history for a household")
    public List<DeterminationResponse> history(@PathVariable Long id) {
        return service.historyFor(id).stream().map(DeterminationResponse::from).toList();
    }
}
