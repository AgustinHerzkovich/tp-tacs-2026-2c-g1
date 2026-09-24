package com.solnotfound.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into RFC 7807 {@link ProblemDetail} responses.
 *
 * <p>Every response includes a {@code code} property with an {@link ErrorCode}. Clients must use
 * that code to decide what to show the user; {@code detail} is a developer-facing message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidActivityException.class)
  public ResponseEntity<ProblemDetail> handleInvalidActivity(InvalidActivityException exception) {
    return respond(HttpStatus.BAD_REQUEST, "Invalid activity", exception);
  }

  @ExceptionHandler(ActivityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleActivityNotFound(ActivityNotFoundException exception) {
    return respond(HttpStatus.NOT_FOUND, "Activity not found", exception);
  }

  @ExceptionHandler(ActivityAccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleActivityAccessDenied(
      ActivityAccessDeniedException exception) {
    return respond(HttpStatus.FORBIDDEN, "Activity access denied", exception);
  }

  @ExceptionHandler(IllegalStateActivityException.class)
  public ResponseEntity<ProblemDetail> handleIllegalStateActivityException(
      IllegalStateActivityException exception) {
    return respond(HttpStatus.CONFLICT, "Activity state conflict", exception);
  }

  @ExceptionHandler(InvalidVotationOptionsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVotationOptions(
      InvalidVotationOptionsException exception) {
    ResponseEntity<ProblemDetail> response =
        respond(HttpStatus.BAD_REQUEST, "Invalid votation options", exception);
    Objects.requireNonNull(response.getBody())
        .setProperty("invalidOptionDates", exception.getInvalidOptionDates());
    return response;
  }

  @ExceptionHandler(InvalidVotationSettingsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVotationSettings(
      InvalidVotationSettingsException exception) {
    return respond(HttpStatus.BAD_REQUEST, "Invalid votation settings", exception);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(
          error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"));
    }

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Request validation failed");
    problem.setProperty("code", ErrorCode.VALIDATION_FAILED);
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + exception.getName() + "'");
    problem.setTitle("Invalid request parameter");
    problem.setProperty("code", ErrorCode.INVALID_PARAMETER);
    problem.setProperty("parameter", exception.getName());
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(InvalidStatisticsRangeException.class)
  public ResponseEntity<ProblemDetail> handleInvalidStatisticsRange(
      InvalidStatisticsRangeException exception) {
    return respond(HttpStatus.BAD_REQUEST, "Invalid statistics range", exception);
  }

  @ExceptionHandler(CouldNotRetrieveStatisticsException.class)
  public ResponseEntity<ProblemDetail> handleStatisticsServiceUnavailable(
      CouldNotRetrieveStatisticsException exception) {
    ResponseEntity<ProblemDetail> response =
        respond(HttpStatus.SERVICE_UNAVAILABLE, "Statistics unavailable", exception);
    ProblemDetail problem = Objects.requireNonNull(response.getBody());
    // Tells the client that retrying later may succeed.
    problem.setProperty("retryable", true);
    problem.setProperty("timestamp", Instant.now());
    return response;
  }

  @ExceptionHandler(InvaildActivityStatusException.class)
  public ResponseEntity<ProblemDetail> handleInvalidActivityStatus(
      InvaildActivityStatusException exception) {
    ResponseEntity<ProblemDetail> response =
        respond(HttpStatus.BAD_REQUEST, "Invalid activity status", exception);
    Objects.requireNonNull(response.getBody()).setProperty("timestamp", Instant.now());
    return response;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException exception) {
    return respond(HttpStatus.NOT_FOUND, "Resource not found", exception);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
    return respond(HttpStatus.FORBIDDEN, "Access denied", exception);
  }

  @ExceptionHandler(WeatherUnavailableException.class)
  public ResponseEntity<ProblemDetail> handleWeatherUnavailable(
      WeatherUnavailableException exception) {
    return respond(HttpStatus.SERVICE_UNAVAILABLE, "Weather service unavailable", exception);
  }

  private ResponseEntity<ProblemDetail> respond(
      HttpStatus status, String title, CodedException exception) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    problem.setTitle(title);
    problem.setProperty("code", exception.getCode());
    return ResponseEntity.status(status).body(problem);
  }
}
