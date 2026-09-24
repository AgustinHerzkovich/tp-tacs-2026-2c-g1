package com.solnotfound.exception;

public class ActivityAccessDeniedException extends CodedException {

  public ActivityAccessDeniedException(String message) {
    super(ErrorCode.NOT_ACTIVITY_MEMBER, message);
  }

  public ActivityAccessDeniedException(ErrorCode code, String message) {
    super(code, message);
  }
}
