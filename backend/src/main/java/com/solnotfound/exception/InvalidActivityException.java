package com.solnotfound.exception;

public class InvalidActivityException extends CodedException {

  public InvalidActivityException(String message) {
    super(ErrorCode.INVALID_ACTIVITY, message);
  }

  public InvalidActivityException(ErrorCode code, String message) {
    super(code, message);
  }
}
