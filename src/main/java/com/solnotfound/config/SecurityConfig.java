package com.solnotfound.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
          .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not configure web security", exception);
    }
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${security.jwt.secret:development-secret-must-be-at-least-32-bytes}") String secret) {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
}
