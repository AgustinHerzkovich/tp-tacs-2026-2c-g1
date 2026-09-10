package com.solnotfound.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(
              requests ->
                  requests
                      .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                      .permitAll()
                      .requestMatchers("/statistics/**")
                      .hasRole("ADMIN")
                      .anyRequest()
                      .authenticated())
          .oauth2ResourceServer(
              resourceServer ->
                  resourceServer.jwt(
                      jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not configure web security", exception);
    }
  }

  @Bean
  Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realmAccess = jwt.getClaim("realm_access");

          if (realmAccess == null) {
            return List.of();
          }

          Object rolesObject = realmAccess.get("roles");

          if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
          }

          return roles.stream()
              .map(Object::toString)
              .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
              .toList();
        });

    return converter;
  }
}
