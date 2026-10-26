package com.solnotfound.adapters.legacy;

/** A call to the legacy weather service that did not produce a usable forecast. */
public class LegacyProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final LegacyFailure failure;

  public LegacyProviderException(LegacyFailure failure, String message) {
    super(message);
    this.failure = failure;
  }

  public LegacyProviderException(LegacyFailure failure, String message, Throwable cause) {
    super(message, cause);
    this.failure = failure;
  }

  public LegacyFailure getFailure() {
    return failure;
  }

  public boolean isRetryable() {
    return failure.isRetryable();
  }

  public boolean isProviderFault() {
    return failure.isProviderFault();
  }
}
