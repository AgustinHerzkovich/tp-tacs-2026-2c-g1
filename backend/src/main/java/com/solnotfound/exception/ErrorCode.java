package com.solnotfound.exception;

/**
 * Stable, machine-readable identifiers for API errors.
 *
 * <p>Every {@code ProblemDetail} returned by {@link GlobalExceptionHandler} carries one of these
 * values in its {@code code} property. Clients must branch on the code, never on the {@code detail}
 * text: the detail is an English developer-facing message that may change and may include
 * identifiers, while the code is part of the API contract. The frontend maps each code to a
 * user-facing message in its own language.
 */
public enum ErrorCode {
  VALIDATION_FAILED,
  INVALID_PARAMETER,
  INVALID_ACTIVITY,
  ACTIVITY_DATE_IN_PAST,
  INVALID_DATE_RANGE,
  TOO_MANY_IMAGES,
  IMAGE_TOO_LARGE,
  INVALID_IMAGE,
  ACTIVITY_NOT_FOUND,
  ACTIVITY_FULL,
  ACTIVITY_CLOSED,
  NOT_ACTIVITY_MEMBER,
  INVALID_ACTIVITY_STATUS,
  VOTATION_NOT_FOUND,
  VOTATION_OPTION_NOT_FOUND,
  VOTATION_CLOSED,
  NOT_ORGANIZER,
  INVALID_VOTATION_OPTIONS,
  INVALID_VOTATION_SETTINGS,
  ACCESS_DENIED,
  RESOURCE_NOT_FOUND,
  WEATHER_UNAVAILABLE,
  STATISTICS_UNAVAILABLE
}
