package com.sgopireddy.ies.events;

import com.sgopireddy.ies.domain.Determination;

/**
 * Publishing is an interface so the determination path does not depend on Kafka
 * being present. Tests and local runs use the logging implementation; a broker
 * is only required where one is actually configured.
 */
public interface DeterminationEventPublisher {
    void publish(Determination determination);
}
