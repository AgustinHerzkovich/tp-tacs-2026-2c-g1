package com.solnotfound.adapters;

import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "weather.provider", havingValue = "in-memory")
public class InMemoryWeatherAdapter implements IWeatherAdapter {

  @Override
  public WeatherForecast getWeather(Location location) {
    return placeholderForecast(LocalDateTime.now());
  }

  @Override
  public WeatherForecast getFutureClimate(Location location, LocalDateTime dateTime) {
    return placeholderForecast(dateTime);
  }

  private WeatherForecast placeholderForecast(LocalDateTime dateTime) {
    return new WeatherForecast(dateTime, 22.0f, 0.0f, 5.0f);
  }
}
