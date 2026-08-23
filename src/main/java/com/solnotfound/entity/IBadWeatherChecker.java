package com.solnotfound.entity;

import org.springframework.context.annotation.Bean;


public interface IBadWeatherChecker {
  public boolean isBadWeather(Weather weather);
}
