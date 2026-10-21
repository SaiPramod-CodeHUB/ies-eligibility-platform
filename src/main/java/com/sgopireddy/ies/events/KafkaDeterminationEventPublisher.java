package com.sgopireddy.ies.events;

import com.sgopireddy.ies.domain.Determination;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes to Kafka, keyed by household id so every event for one household
 * lands on the same partition and stays in order.
 *
 * Send failures are logged, not rethrown. The determination is already
 * committed and legally valid; failing the caller's request because a broker is
 * unavailable would be the wrong trade. A real deployment pairs this with a
 * transactional outbox so the event is retried rather than lost.
 */
@Component
@ConditionalOnProperty(name = "ies.events.publisher", havingValue = "kafka")
public class KafkaDeterminationEventPublisher implements DeterminationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeterminationEventPublisher.class);
    public static final String TOPIC = "ies.determinations.v1";

    private final KafkaTemplate<String, DeterminationEvent> kafka;

    public KafkaDeterminationEventPublisher(KafkaTemplate<String, DeterminationEvent> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(Determination determination) {
        DeterminationEvent event = DeterminationEvent.from(determination, UUID.randomUUID().toString());
        kafka.send(TOPIC, event.partitionKey(), event)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        log.error("Failed to publish determination {} to {}: {}",
                                determination.getId(), TOPIC, failure.getMessage());
                    } else {
                        log.info("Published determination {} for household {} to {} partition {}",
                                determination.getId(), determination.getHouseholdId(),
                                TOPIC, result.getRecordMetadata().partition());
                    }
                });
    }
}
