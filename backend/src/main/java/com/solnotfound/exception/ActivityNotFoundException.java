package com.solnotfound.exception;

public class ActivityNotFoundException extends CodedException {

  public ActivityNotFoundException(String message) {
    super(ErrorCode.ACTIVITY_NOT_FOUND, message);
  }

  public ActivityNotFoundException(ErrorCode code, String message) {
    super(code, message);
  }
}
