package com.solnotfound.entity.weather;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaxWindCondition implements WeatherCondition {
  private final Double maxWindSpeed;

  @Override
  public Boolean isSatisfiedBy(WeatherForecast weatherForecast) {
    return weatherForecast.getWindSpeed() != null && weatherForecast.getWindSpeed() <= maxWindSpeed;
  }
}
