package com.solnotfound.exception;

/** Indicates that weather information cannot currently be obtained from the configured provider. */
public class WeatherUnavailableException extends RuntimeException {

  public WeatherUnavailableException(String message) {
    super(message);
  }

  public WeatherUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
