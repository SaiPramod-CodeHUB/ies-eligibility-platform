package com.sgopireddy.ies.notices;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/determinations/{determinationId}/notice")
@Tag(name = "Notices", description = "AI-drafted notices, grounded in the determination's audit trail")
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    public record NoticeResponse(Long noticeId, Long determinationId, Long householdId,
                                 String body, String generatedBy, Instant generatedAt,
                                 List<String> citations) {
        static NoticeResponse from(Notice n) {
            return new NoticeResponse(n.getId(), n.getDeterminationId(), n.getHouseholdId(),
                    n.getBody(), n.getGeneratedBy(), n.getGeneratedAt(), n.getCitations());
        }
    }

    @PostMapping
    @Operation(summary = "Draft the notice for a determination",
               description = "Idempotent: returns the existing notice if one has already been drafted.")
    public ResponseEntity<NoticeResponse> generate(@PathVariable Long determinationId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NoticeResponse.from(service.generateFor(determinationId)));
    }

    @GetMapping
    @Operation(summary = "Fetch the notice for a determination")
    public NoticeResponse get(@PathVariable Long determinationId) {
        return NoticeResponse.from(service.getForDetermination(determinationId));
    }
}
