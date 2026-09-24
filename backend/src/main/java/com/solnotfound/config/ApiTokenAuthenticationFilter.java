package com.solnotfound.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates machine clients that present a shared static API token. */
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Api-Token";

  private static final Logger log = LoggerFactory.getLogger(ApiTokenAuthenticationFilter.class);

  private final byte[] expectedToken;
  private final String principal;
  private final String role;

  public ApiTokenAuthenticationFilter(String expectedToken, String principal, String role) {
    this.expectedToken =
        expectedToken == null ? new byte[0] : expectedToken.getBytes(StandardCharsets.UTF_8);
    this.principal = principal;
    this.role = role;
    if (this.expectedToken.length == 0) {
      log.warn("No API token configured for {}; every request from it will be rejected", principal);
    }
  }

  /**
   * Authenticates the request as the configured principal when the {@value #HEADER} header matches
   * the expected token. The comparison is constant-time. When no token is configured every request
   * stays unauthenticated, so the protected endpoints fail closed. Requests without a valid token
   * continue unauthenticated and are rejected later by the authorization rules.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String providedToken = request.getHeader(HEADER);
    if (expectedToken.length > 0
        && providedToken != null
        && MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8))) {
      SecurityContextHolder.getContext()
          .setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated(
                  principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    filterChain.doFilter(request, response);
  }
}
