package com.solnotfound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      return http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(
              requests ->
                  requests
                      .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                      .permitAll()
                      .requestMatchers("/statistics/**")
                      .hasRole("ADMIN")
                      .anyRequest()
                      .permitAll())
          .oauth2ResourceServer(
              resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not configure web security", exception);
    }
  }
}