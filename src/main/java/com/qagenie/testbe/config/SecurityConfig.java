package com.qagenie.testbe.config;

import com.qagenie.testbe.security.JwtAuthFilter;
import com.qagenie.testbe.security.Role;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Stateless JWT security. Endpoint-level role checks are additionally
 * enforced with @PreAuthorize at the controller/service layer so the
 * exact same rule set is documented next to each operation.
 *
 * qagenie.security.jwt.enabled toggles the two filterChain beans below:
 * true (default) enforces real JWT auth as described above; false permits
 * every request and grants anonymous callers every Role so @PreAuthorize
 * checks still pass - lets the rest of the app be exercised without
 * needing to mint tokens first, flip back once auth is ready.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .disable())
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // Required for H2 Console
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.security.jwt", name = "enabled", havingValue = "false")
    public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
        String[] anonymousAuthorities = Arrays.stream(Role.values())
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toList())
                .toArray(new String[0]);

        http
                .csrf(csrf -> csrf.disable())
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .anonymous(anon -> anon.authorities(anonymousAuthorities))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}