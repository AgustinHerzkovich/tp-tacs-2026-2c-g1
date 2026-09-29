package com.solnotfound.exception;

public class ImageStorageException extends CodedException {
  public ImageStorageException(String message, Throwable cause) {
    super(ErrorCode.IMAGE_STORAGE_UNAVAILABLE, message, cause);
  }
}
