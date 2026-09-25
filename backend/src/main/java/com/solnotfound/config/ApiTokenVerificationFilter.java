package com.solnotfound.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Second factor of the machine clients: verifies the shared static API token sent in the {@value
 * #HEADER} header. The filter grants no identity by itself, so the resource server still has to
 * validate the Keycloak JWT; a request is accepted only when both credentials are present.
 */
public class ApiTokenVerificationFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Api-Token";

  private static final Logger log = LoggerFactory.getLogger(ApiTokenVerificationFilter.class);

  private final byte[] expectedToken;
  private final String principal;

  public ApiTokenVerificationFilter(String expectedToken, String principal) {
    this.expectedToken =
        expectedToken == null ? new byte[0] : expectedToken.getBytes(StandardCharsets.UTF_8);
    this.principal = principal;
    if (this.expectedToken.length == 0) {
      log.warn("No API token configured for {}; every request from it will be rejected", principal);
    }
  }

  /**
   * Rejects with 401 the requests that do not present the expected token, so a machine client
   * cannot rely on its JWT alone. The comparison is constant-time and requests without the header
   * fail closed. Requests that pass continue unauthenticated, and the JWT filter authenticates
   * them.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String providedToken = request.getHeader(HEADER);
    if (expectedToken.length == 0
        || providedToken == null
        || !MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8))) {
      log.debug(
          "Rejected request to {} from {}: missing or invalid API token",
          request.getRequestURI(),
          principal);
      response.sendError(HttpStatus.UNAUTHORIZED.value());
      return;
    }
    filterChain.doFilter(request, response);
  }
}
