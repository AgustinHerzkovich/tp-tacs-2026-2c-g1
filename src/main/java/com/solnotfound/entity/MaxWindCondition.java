package com.solnotfound.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaxWindCondition implements WeatherCondition {
  private final Double maxWindSpeed;

  @Override
  public Boolean isSatisfiedBy(WeatherForecast weatherForecast) {
    return true; // TODO
  }
}
