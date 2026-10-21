package com.sgopireddy.ies.api;

import com.sgopireddy.ies.api.dto.ApiError;
import com.sgopireddy.ies.ai.LlmUnavailableException;
import com.sgopireddy.ies.engine.*;
import com.sgopireddy.ies.jurisdiction.*;
import com.sgopireddy.ies.notices.*;
import com.sgopireddy.ies.thresholds.NoThresholdsInForceException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** One error contract for the whole API rather than try/catch per controller. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({HouseholdNotFoundException.class, DeterminationNotFoundException.class,
                       NoticeNotFoundException.class})
    public ResponseEntity<ApiError> notFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    /**
     * The jurisdiction is not configured. A 400 rather than a 404: the
     * household record names a jurisdiction the deployment does not know, which
     * is a bad input, not a missing resource.
     */
    @ExceptionHandler(UnknownJurisdictionException.class)
    public ResponseEntity<ApiError> unknownJurisdiction(UnknownJurisdictionException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    /**
     * The jurisdiction is known but does not administer the program asked for.
     * 409 rather than 400: the request is well formed and the program exists —
     * it just is not offered here.
     */
    @ExceptionHandler(ProgramNotOfferedException.class)
    public ResponseEntity<ApiError> notOffered(ProgramNotOfferedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(UnsupportedProgramException.class)
    public ResponseEntity<ApiError> unsupported(UnsupportedProgramException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    /**
     * A notice whose figures are not grounded in the determination is a defect,
     * not a bad request — surfaced as 502 so it is alerted on rather than
     * silently returned to a caseworker.
     */
    @ExceptionHandler(UngroundedNoticeException.class)
    public ResponseEntity<ApiError> ungrounded(UngroundedNoticeException ex) {
        return build(HttpStatus.BAD_GATEWAY,
                "Generated notice failed the grounding check and was discarded",
                ex.getUngroundedValues());
    }

    /** The determination stands even when the model is down; only the notice is unavailable. */
    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ApiError> llmDown(LlmUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NoThresholdsInForceException.class)
    public ResponseEntity<ApiError> noThresholds(NoThresholdsInForceException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(
                new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
    }
}
