package com.solnotfound.entity.weather;

public interface WeatherCondition {
  Boolean isSatisfiedBy(WeatherForecast weatherForecast);
}
