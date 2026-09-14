package com.solnotfound.entity.weather;

import com.solnotfound.entity.activity.Activity;

public interface IBadWeatherChecker {
  public boolean isBadWeatherForActivity(WeatherForecast weather, Activity activity);
}
