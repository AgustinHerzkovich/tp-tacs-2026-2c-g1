package com.solnotfound.adapters.legacy;

/**
 * No usable forecast could be obtained from the legacy provider, either because it failed or
 * because a protection (circuit breaker, rate limiter, bulkhead) refused the call. Callers fall
 * back to the main provider.
 */
public class LegacyProviderUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LegacyProviderUnavailableException(String message) {
    super(message);
  }

  public LegacyProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
