package com.sgopireddy.ies.thresholds;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-jurisdiction threshold overrides, as configuration.
 *
 * Poverty guidelines are federal and shared. Resource limits are set locally,
 * so a state that differs from the baseline declares only what differs:
 *
 * <pre>
 * ies.thresholds.overrides:
 *   NY:
 *     FOOD_RESOURCE_LIMIT: 2750
 *   TX:
 *     FOOD_RESOURCE_LIMIT: 5000
 * </pre>
 */
@ConfigurationProperties(prefix = "ies.thresholds")
public class ThresholdOverrideProperties {

    private Map<String, Map<String, BigDecimal>> overrides = new LinkedHashMap<>();

    public Map<String, Map<String, BigDecimal>> getOverrides() { return overrides; }
    public void setOverrides(Map<String, Map<String, BigDecimal>> v) { this.overrides = v; }
}
