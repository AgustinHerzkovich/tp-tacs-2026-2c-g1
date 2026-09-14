package com.solnotfound.adapters;

import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import java.time.LocalDateTime;
import java.util.List;

public interface IWeatherAdapter {
  WeatherForecast getWeather(Location location);

  WeatherForecast getFutureClimate(Location location, LocalDateTime dateTime);

  /**
   * Returns forecasts for the requested instants. Implementations may override this method to
   * retrieve the complete range with a single provider request.
   *
   * @param location activity location
   * @param dateTimes requested local date-times
   * @return forecasts in the same order as the requested date-times
   * @throws com.solnotfound.exception.WeatherUnavailableException when any forecast is unavailable
   */
  default List<WeatherForecast> getForecastRange(Location location, List<LocalDateTime> dateTimes) {
    return dateTimes.stream().map(dateTime -> getFutureClimate(location, dateTime)).toList();
  }
}
