package com.sgopireddy.ies.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "ies.events.publisher", havingValue = "kafka")
public class KafkaTopicConfig {

    @Bean
    public NewTopic determinationsTopic() {
        return TopicBuilder.name(KafkaDeterminationEventPublisher.TOPIC)
                .partitions(6)
                .replicas(3)
                // Partition count is the ceiling on consumer parallelism within a
                // group; replicas=3 with min.insync.replicas=2 is what makes
                // acks=all actually durable.
                .config("min.insync.replicas", "2")
                .build();
    }
}
