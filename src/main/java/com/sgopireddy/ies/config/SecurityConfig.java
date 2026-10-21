package com.sgopireddy.ies.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Role model:
 *
 *   CASEWORKER  — intake, run determinations, read anything, use /assist
 *   SUPERVISOR  — additionally issue notices to households
 *
 * Issuing a notice is separated because it is the act that starts the appeal
 * clock. Running a determination is reversible; sending the household a legal
 * notice is not, so it sits behind a second role.
 *
 * DEMONSTRATION AUTHENTICATION. In-memory users with HTTP Basic, stateless. A
 * real deployment authenticates against the agency directory and carries
 * identity as a signed token; nothing here is a model for production auth.
 */
@Configuration
public class SecurityConfig {

    public static final String CASEWORKER = "CASEWORKER";
    public static final String SUPERVISOR = "SUPERVISOR";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // No browser sessions and no CSRF: this is a stateless JSON API
            // consumed by services, not a form-rendering web application.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Issuing a notice starts the appeal clock — supervisor only.
                .requestMatchers(HttpMethod.POST, "/api/v1/determinations/*/notice")
                    .hasRole(SUPERVISOR)

                // Indexing policy material changes what every caseworker is told.
                .requestMatchers(HttpMethod.POST, "/api/v1/assist/ingest")
                    .hasRole(SUPERVISOR)

                .requestMatchers("/api/v1/**").hasAnyRole(CASEWORKER, SUPERVISOR)
                .anyRequest().authenticated())
            .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        UserDetails caseworker = User.withUsername("caseworker")
                .password(encoder.encode("caseworker"))
                .roles(CASEWORKER)
                .build();

        UserDetails supervisor = User.withUsername("supervisor")
                .password(encoder.encode("supervisor"))
                .roles(SUPERVISOR)
                .build();

        return new InMemoryUserDetailsManager(caseworker, supervisor);
    }
}
