package com.solnotfound.exception;

public class InvalidActivityStatusTransitionException extends CodedException {

  public InvalidActivityStatusTransitionException(String message) {
    super(ErrorCode.INVALID_STATUS_TRANSITION, message);
  }
}
