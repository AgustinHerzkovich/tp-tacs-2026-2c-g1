package com.solnotfound.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaxRainProbabilityCondition implements WeatherCondition {
  private final Integer maxProbability;

  @Override
  public Boolean isSatisfiedBy(WeatherForecast weatherForecast) {
    return true; // TODO
  }
}
