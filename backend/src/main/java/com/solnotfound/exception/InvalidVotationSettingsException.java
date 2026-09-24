package com.solnotfound.exception;

public class InvalidVotationSettingsException extends CodedException {

  public InvalidVotationSettingsException(String message) {
    super(ErrorCode.INVALID_VOTATION_SETTINGS, message);
  }

  public InvalidVotationSettingsException(ErrorCode code, String message) {
    super(code, message);
  }
}
