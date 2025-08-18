package com.example.complaintbackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**") // disable CSRF for H2 + API
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // allow H2 console in iframe
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll() // allow H2 console
                .requestMatchers("/api/**").permitAll()       // allow your REST API
                .anyRequest().authenticated()                 // protect other endpoints
            )
            .httpBasic(Customizer.withDefaults()); // still enable basic auth if needed
        return http.build();
    }
}
