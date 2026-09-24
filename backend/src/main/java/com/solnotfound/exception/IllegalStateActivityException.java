package com.solnotfound.exception;

public class IllegalStateActivityException extends CodedException {

  public IllegalStateActivityException(String message) {
    super(ErrorCode.ACTIVITY_CLOSED, message);
  }

  public IllegalStateActivityException(ErrorCode code, String message) {
    super(code, message);
  }
}
