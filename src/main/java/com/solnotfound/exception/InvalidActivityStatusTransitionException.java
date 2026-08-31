package com.solnotfound.exception;

public class InvalidActivityStatusTransitionException extends RuntimeException {

  public InvalidActivityStatusTransitionException(String message) {
    super(message);
  }
}
