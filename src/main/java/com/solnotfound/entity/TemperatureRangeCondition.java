package com.solnotfound.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemperatureRangeCondition implements WeatherCondition {
  private final Integer minTemperature;
  private final Integer maxTemperature;

  @Override
  public Boolean isSatisfiedBy(WeatherForecast weatherForecast) {
    Float temperature = weatherForecast.getTemperature();
    return temperature != null && temperature >= minTemperature && temperature <= maxTemperature;
  }
}
