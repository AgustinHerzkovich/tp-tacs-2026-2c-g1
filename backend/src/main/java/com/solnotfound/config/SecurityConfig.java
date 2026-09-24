package com.solnotfound.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain schedulerSecurityFilterChain(
      HttpSecurity http,
      @org.springframework.beans.factory.annotation.Value("${scheduler.oidc.audience}")
          String audience,
      @org.springframework.beans.factory.annotation.Value("${scheduler.oidc.email}") String email) {
    try {
      JwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
      org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> validator =
          new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
              new JwtIssuerValidator("https://accounts.google.com"),
              token ->
                  token.getAudience().contains(audience)
                          && email.equals(token.getClaimAsString("email"))
                      ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
                          .success()
                      : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                          new org.springframework.security.oauth2.core.OAuth2Error(
                              "invalid_token", "Invalid scheduler identity", null)));
      ((org.springframework.security.oauth2.jwt.NimbusJwtDecoder) decoder)
          .setJwtValidator(validator);

      return http.securityMatcher("/internal/scheduled/**")
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
          .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(decoder)))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not configure scheduler security", exception);
    }
  }

  /**
   * Protects the Telegram bot endpoints with a static API token sent in the {@value
   * ApiTokenAuthenticationFilter#HEADER} header instead of a Keycloak JWT. Requests without a valid
   * token receive 401. When no token is configured, every request to these endpoints is rejected.
   */
  @Bean
  @Order(2)
  SecurityFilterChain telegramSecurityFilterChain(
      HttpSecurity http,
      @org.springframework.beans.factory.annotation.Value("${telegram.api-token}")
          String apiToken) {
    try {
      return http.securityMatcher("/users/telegram/**")
          .csrf(csrf -> csrf.disable())
          .sessionManagement(
              session ->
                  session.sessionCreationPolicy(
                      org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
          .addFilterBefore(
              new ApiTokenAuthenticationFilter(apiToken, "telegram-bot", "TELEGRAM_BOT"),
              org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
          .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("TELEGRAM_BOT"))
          .exceptionHandling(
              exceptions ->
                  exceptions.authenticationEntryPoint(
                      new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                          org.springframework.http.HttpStatus.UNAUTHORIZED)))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not configure Telegram security", exception);
    }
  }

  @Bean
  @Order(3)
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(
              requests ->
                  requests
                      .requestMatchers("/healthcheck", "/swagger-ui/**", "/v3/api-docs/**")
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
