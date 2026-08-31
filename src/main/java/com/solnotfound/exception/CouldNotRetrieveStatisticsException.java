package com.solnotfound.exception;

public class CouldNotRetrieveStatisticsException extends RuntimeException {
  public CouldNotRetrieveStatisticsException(String message, Exception e) {
    super(message, e);
  }

  public CouldNotRetrieveStatisticsException(String message) {
    super(message);
  }
}
