package com.solnotfound.entity;

public interface WeatherCondition {
  Boolean isSatisfiedBy(WeatherForecast weatherForecast);
}
