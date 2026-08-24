package com.solnotfound.adapters;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.solnotfound.entity.Location;
import com.solnotfound.entity.WeatherForecast;

// se hace esre in memory weather adapter ya que sino spring no buildea el docker
@Component
public class InMemoryWeatherAdapter implements IWeatherAdapter {

  @Override
  public WeatherForecast getWeather(Location location) {
    return placeholderForecast(LocalDateTime.now());
  }

  @Override
  public WeatherForecast getClimate(Location location) {
    return placeholderForecast(LocalDateTime.now());
  }

  @Override
  public WeatherForecast getFutureClimate(Location location, LocalDateTime dateTime) {
    return placeholderForecast(dateTime);
  }

  private WeatherForecast placeholderForecast(LocalDateTime dateTime) {
    return new WeatherForecast(null, dateTime, 22.0f, 0.0f, 5.0f);
  }
}
