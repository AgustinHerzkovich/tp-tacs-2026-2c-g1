package com.solnotfound.exception;

public class InvalidTelegramLinkCodeException extends CodedException {

  public InvalidTelegramLinkCodeException(String message) {
    super(ErrorCode.TELEGRAM_LINK_CODE_INVALID, message);
  }
}
