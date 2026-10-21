package com.sgopireddy.ies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Jurisdictions and threshold overrides are bound from configuration, so the
 * @ConfigurationProperties classes are scanned rather than registered one by
 * one — adding a jurisdiction never means touching this class.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class IesApplication {
    public static void main(String[] args) {
        SpringApplication.run(IesApplication.class, args);
    }
}
