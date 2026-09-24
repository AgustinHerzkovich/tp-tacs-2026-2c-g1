package com.solnotfound.exception;

public class InvaildActivityStatusException extends CodedException {

  public InvaildActivityStatusException(String message) {
    super(ErrorCode.INVALID_ACTIVITY_STATUS, message);
  }

  public InvaildActivityStatusException(ErrorCode code, String message) {
    super(code, message);
  }

  public InvaildActivityStatusException(String message, Throwable cause) {
    super(ErrorCode.INVALID_ACTIVITY_STATUS, message, cause);
  }
}
