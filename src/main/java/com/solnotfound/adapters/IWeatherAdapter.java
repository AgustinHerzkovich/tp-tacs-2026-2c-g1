package com.solnotfound.adapters;

import com.solnotfound.entity.Location;
import com.solnotfound.entity.WeatherForecast;
import java.time.LocalDateTime;

public interface IWeatherAdapter {
  public WeatherForecast getWeather(Location location);

  WeatherForecast getClimate(Location location);

  WeatherForecast getFutureClimate(Location location, LocalDateTime dateTime);
}
