package com.solnotfound.exception;

public class InvalidPageRequestException extends CodedException {

  public InvalidPageRequestException(String message) {
    super(ErrorCode.INVALID_PARAMETER, message);
  }
}
