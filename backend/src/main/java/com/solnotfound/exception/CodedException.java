package com.solnotfound.exception;

/**
 * Base class for business exceptions that are exposed through the API.
 *
 * <p>Each exception carries an {@link ErrorCode} that {@link GlobalExceptionHandler} publishes as
 * the {@code code} property of the response. Subclasses define a default code for their general
 * case and accept a more specific one when the caller knows the exact reason (for example, {@link
 * ErrorCode#ACTIVITY_FULL} instead of the generic {@link ErrorCode#ACTIVITY_CLOSED}).
 */
public abstract class CodedException extends RuntimeException {

  private final ErrorCode code;

  protected CodedException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  protected CodedException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public ErrorCode getCode() {
    return code;
  }
}
