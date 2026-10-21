package com.sgopireddy.ies.events;

import com.sgopireddy.ies.domain.Determination;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Default publisher: no broker required, so tests and local runs need no Kafka. */
@Component
@ConditionalOnMissingBean(KafkaDeterminationEventPublisher.class)
public class LoggingDeterminationEventPublisher implements DeterminationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDeterminationEventPublisher.class);

    @Override
    public void publish(Determination d) {
        DeterminationEvent e = DeterminationEvent.from(d, UUID.randomUUID().toString());
        log.info("determination.event household={} jurisdiction={} program={} outcome={} reasons={}",
                e.householdId(), e.jurisdictionCode(), e.programType(), e.outcome(), e.reasons().size());
    }
}
