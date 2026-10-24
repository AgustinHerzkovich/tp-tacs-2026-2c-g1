package com.solnotfound.adapters.legacy;

/**
 * Ways a call to the legacy weather service can fail. Each one was observed while probing the
 * binary (see the README section on discovered failures); the client tells them apart so they can
 * be counted and so only the transient ones are retried.
 */
public enum LegacyFailure {
  /** The service did not answer within the time budget of an attempt. */
  TIMEOUT(true, true),
  /**
   * The connection was refused or the host did not resolve: the process is down or restarting. Not
   * retried: the next attempt comes 100 to 200 ms later and the process takes seconds to come back,
   * so retrying only delays the fallback. It still counts against the provider's health.
   */
  UNREACHABLE(false, true),
  /** An HTTP status other than 200. */
  HTTP_ERROR(true, true),
  /** The body is not valid JSON, even though the status was 200. */
  MALFORMED_RESPONSE(true, true),
  /** The envelope answers a different request id. */
  MISMATCHED_ID(true, true),
  /** The envelope reports {@code status: "ERROR"} for a transient reason. */
  PROVIDER_ERROR(true, true),
  /** The envelope reports an error caused by the request itself; repeating it cannot help. */
  REJECTED_REQUEST(false, false),
  /** {@code status: "OK"} but the result is missing fields or has impossible values. */
  INVALID_RESULT(true, true);

  private final boolean retryable;
  private final boolean providerFault;

  LegacyFailure(boolean retryable, boolean providerFault) {
    this.retryable = retryable;
    this.providerFault = providerFault;
  }

  public boolean isRetryable() {
    return retryable;
  }

  public boolean isProviderFault() {
    return providerFault;
  }
}
