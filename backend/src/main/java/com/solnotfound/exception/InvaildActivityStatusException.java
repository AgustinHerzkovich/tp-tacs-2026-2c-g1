package com.solnotfound.exception;

public class InvaildActivityStatusException extends RuntimeException {
  public InvaildActivityStatusException(String message) {
    super(message);
  }

  public InvaildActivityStatusException(String message, Throwable cause) {
    super(message, cause);
  }
}
