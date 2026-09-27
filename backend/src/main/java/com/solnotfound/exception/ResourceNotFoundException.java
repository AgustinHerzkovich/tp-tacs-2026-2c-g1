package com.solnotfound.exception;

public class ResourceNotFoundException extends CodedException {

  public ResourceNotFoundException(String message) {
    super(ErrorCode.RESOURCE_NOT_FOUND, message);
  }

  public ResourceNotFoundException(ErrorCode code, String message) {
    super(code, message);
  }
}
