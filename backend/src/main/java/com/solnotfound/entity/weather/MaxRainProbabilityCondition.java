package com.solnotfound.entity.weather;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaxRainProbabilityCondition implements WeatherCondition {
  private final Integer maxProbability;

  @Override
  public Boolean isSatisfiedBy(WeatherForecast weatherForecast) {
    return weatherForecast.getChanceOfRain() != null
        && weatherForecast.getChanceOfRain() <= maxProbability;
  }
}
