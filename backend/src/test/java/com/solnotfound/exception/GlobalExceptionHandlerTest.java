package com.solnotfound.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void imageStorageFailuresAreCodedServiceUnavailable() {
    ResponseEntity<ProblemDetail> response =
        handler.handleImageStorage(new ImageStorageException("Could not store image", null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties())
        .containsEntry("code", ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
  }

  @Test
  void invalidStatusTransitionsAreCodedConflicts() {
    ResponseEntity<ProblemDetail> response =
        handler.handleInvalidStatusTransition(
            new InvalidActivityStatusTransitionException("Cannot go from FINISHED to PROPOSED"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties())
        .containsEntry("code", ErrorCode.INVALID_STATUS_TRANSITION);
  }

  @Test
  void invalidPageRequestsAreCodedBadRequests() {
    ResponseEntity<ProblemDetail> response =
        handler.handleInvalidPageRequest(new InvalidPageRequestException("size must be <= 100"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties())
        .containsEntry("code", ErrorCode.INVALID_PARAMETER);
  }
}
