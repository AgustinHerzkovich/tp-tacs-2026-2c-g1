package com.solnotfound.exception;

public class AccessDeniedException extends CodedException {

  public AccessDeniedException(String message) {
    super(ErrorCode.ACCESS_DENIED, message);
  }

  public AccessDeniedException(ErrorCode code, String message) {
    super(code, message);
  }
}
