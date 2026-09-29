package com.solnotfound.exception;

public class InvalidStatisticsRangeException extends CodedException {

  public InvalidStatisticsRangeException(String message) {
    super(ErrorCode.INVALID_DATE_RANGE, message);
  }

  public InvalidStatisticsRangeException(ErrorCode code, String message) {
    super(code, message);
  }
}
