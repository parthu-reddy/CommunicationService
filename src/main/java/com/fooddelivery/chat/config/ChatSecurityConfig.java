package com.fooddelivery.chat.config;

import com.fooddelivery.common.security.SecurityContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Chat Service.
 * Overrides the CommonSecurityConfig to add WebSocket endpoint permissions.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(SecurityContextFilter.class)
public class ChatSecurityConfig {

    private final SecurityContextFilter securityContextFilter;

    public ChatSecurityConfig(SecurityContextFilter securityContextFilter) {
        this.securityContextFilter = securityContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(securityContextFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // WebSocket endpoints must be accessible for the handshake
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
