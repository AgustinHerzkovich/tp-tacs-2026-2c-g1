package com.solnotfound.exception;

/** Indicates that weather information cannot currently be obtained from the configured provider. */
public class WeatherUnavailableException extends CodedException {

  public WeatherUnavailableException(String message) {
    super(ErrorCode.WEATHER_UNAVAILABLE, message);
  }

  public WeatherUnavailableException(String message, Throwable cause) {
    super(ErrorCode.WEATHER_UNAVAILABLE, message, cause);
  }
}
