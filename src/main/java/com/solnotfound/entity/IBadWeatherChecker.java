package com.solnotfound.entity;

public interface IBadWeatherChecker {
  public boolean isBadWeatherForActivity(WeatherForecast weather, Activity activity);
}
