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

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidActivityException.class)
  public ResponseEntity<ProblemDetail> handleInvalidActivity(InvalidActivityException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid activity");
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(ActivityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleActivityNotFound(ActivityNotFoundException exception) {

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());

    problem.setTitle("Activity not found");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(ActivityAccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleActivityAccessDenied(
      ActivityAccessDeniedException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    problem.setTitle("Activity access denied");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(IllegalStateActivityException.class)
  public ResponseEntity<ProblemDetail> handleIllegalStateActivityException(
      IllegalStateActivityException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    problem.setTitle("Activity state conflict");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(InvalidVotationOptionsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVotationOptions(
      InvalidVotationOptionsException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid votation options");
    problem.setProperty("invalidOptionDates", exception.getInvalidOptionDates());
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(InvalidVotationSettingsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVotationSettings(
      InvalidVotationSettingsException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid votation settings");
    return ResponseEntity.badRequest().body(problem);
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
    problem.setProperty("parameter", exception.getName());
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(CouldNotRetrieveStatisticsException.class)
  public ProblemDetail handleStatisticsServiceUnavailable(CouldNotRetrieveStatisticsException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    problemDetail.setTitle("Servicio No Disponible");

    // Es buena práctica indicarle al cliente si vale la pena reintentar
    problemDetail.setProperty("retryable", true);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(InvaildActivityStatusException.class)
  public ProblemDetail handleInvalidActivityStatus(InvaildActivityStatusException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Invalid Activity Status");
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Resource not found");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    problem.setTitle("Access denied");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(WeatherUnavailableException.class)
  public ResponseEntity<ProblemDetail> handleWeatherUnavailable(
      WeatherUnavailableException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    problem.setTitle("Weather service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
  }
}
