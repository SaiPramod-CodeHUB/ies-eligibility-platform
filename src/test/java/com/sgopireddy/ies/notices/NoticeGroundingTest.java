package com.sgopireddy.ies.notices;

import com.sgopireddy.ies.ai.*;
import com.sgopireddy.ies.domain.*;
import com.sgopireddy.ies.engine.EligibilityService;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The grounding guard is the point of the notice layer, so it is tested
 * directly: a model that invents a figure must not get that figure stored.
 */
@DisplayName("Notice grounding")
class NoticeGroundingTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    private EligibilityService eligibility;
    private NoticeRepository notices;

    private static Determination ineligibleDetermination() {
        return new Determination(7L, "NY", ProgramType.FOOD_ASSISTANCE,
                "Supplemental Nutrition Assistance Program (SNAP)",
                Outcome.INELIGIBLE, "2026.10.1/rules-baseline/thresholds-2026.1",
                List.of(new FiredRule("FOOD_GROSS_INCOME_TEST", "FAIL",
                        "Gross monthly income $3000.00 exceeds the 130% FPL limit of $2887.30 "
                        + "for a household of 3 (thresholds 2026.1).", "FOOD-GIT-130")),
                FIXED.instant());
    }

    @BeforeEach
    void setUp() {
        eligibility = mock(EligibilityService.class);
        notices = mock(NoticeRepository.class);
        when(eligibility.getDetermination(1L)).thenReturn(ineligibleDetermination());
        when(notices.findByDeterminationId(1L)).thenReturn(Optional.empty());
        when(notices.save(any(Notice.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("a notice built from the facts is accepted and cites the policy")
    void groundedNoticeIsStored() {
        var service = new NoticeService(eligibility, notices, new TemplateLlmClient(), FIXED);

        Notice n = service.generateFor(1L);

        assertThat(n.getBody()).contains("2887.30").contains("not eligible");
        assertThat(n.getCitations()).containsExactly("FOOD-GIT-130");
        assertThat(n.getGeneratedBy()).isEqualTo("template-deterministic-v1");
        verify(notices).save(any(Notice.class));
    }

    @Test
    @DisplayName("a notice that invents a dollar figure is REJECTED, not stored")
    void ungroundedFigureIsRejected() {
        LlmClient hallucinating = new LlmClient() {
            @Override public String complete(String system, String user) {
                // $4,100.00 appears nowhere in the determination
                return "Your household is not eligible because your income of $4,100.00 is too high.";
            }
            @Override public String modelId() { return "test-hallucinating"; }
        };

        var service = new NoticeService(eligibility, notices, hallucinating, FIXED);

        assertThatThrownBy(() -> service.generateFor(1L))
                .isInstanceOf(UngroundedNoticeException.class)
                .hasMessageContaining("4,100.00");

        verify(notices, never()).save(any());
    }

    @Test
    @DisplayName("figures that ARE in the facts pass regardless of comma formatting")
    void formattingDoesNotCauseFalsePositives() {
        LlmClient reformatting = new LlmClient() {
            @Override public String complete(String system, String user) {
                return "Your income of $3000.00 exceeds the limit of $2,887.30.";
            }
            @Override public String modelId() { return "test-reformatting"; }
        };

        var service = new NoticeService(eligibility, notices, reformatting, FIXED);
        assertThat(service.generateFor(1L).getBody()).contains("2,887.30");
    }

    @Test
    @DisplayName("generation is idempotent — a second call returns the stored notice")
    void idempotent() {
        Notice existing = new Notice(1L, 7L, "already drafted", "x", List.of("SNAP-GIT-130"), FIXED.instant());
        when(notices.findByDeterminationId(1L)).thenReturn(Optional.of(existing));

        var service = new NoticeService(eligibility, notices, new TemplateLlmClient(), FIXED);

        assertThat(service.generateFor(1L).getBody()).isEqualTo("already drafted");
        verify(notices, never()).save(any());
    }

    @Test
    @DisplayName("a model failure surfaces as LlmUnavailable — the determination still stands")
    void modelFailureIsSurfaced() {
        LlmClient broken = new LlmClient() {
            @Override public String complete(String s, String u) { throw new IllegalStateException("502"); }
            @Override public String modelId() { return "test-broken"; }
        };

        var service = new NoticeService(eligibility, notices, broken, FIXED);

        assertThatThrownBy(() -> service.generateFor(1L))
                .isInstanceOf(LlmUnavailableException.class);
        verify(notices, never()).save(any());
    }
}
