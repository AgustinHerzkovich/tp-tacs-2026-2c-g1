package com.solnotfound.exception;

public class CouldNotRetrieveStatisticsException extends CodedException {

  public CouldNotRetrieveStatisticsException(String message) {
    super(ErrorCode.STATISTICS_UNAVAILABLE, message);
  }

  public CouldNotRetrieveStatisticsException(ErrorCode code, String message) {
    super(code, message);
  }

  public CouldNotRetrieveStatisticsException(String message, Exception e) {
    super(ErrorCode.STATISTICS_UNAVAILABLE, message, e);
  }
}
