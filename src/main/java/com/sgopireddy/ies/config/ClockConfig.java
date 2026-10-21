package com.sgopireddy.ies.config;

import org.springframework.context.annotation.*;
import java.time.Clock;

/**
 * The clock is injected rather than called statically, so tests can pin a fixed
 * instant and assert that the same snapshot always yields the same determination.
 */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() { return Clock.systemUTC(); }
}
