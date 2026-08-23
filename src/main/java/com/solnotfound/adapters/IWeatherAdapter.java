package com.solnotfound.adapters;

import com.solnotfound.entity.Weather;
import com.solnotfound.entity.Location;

import java.time.LocalDateTime;

public interface IWeatherAdapter {
  public String getWeather(Location location);

  Weather getClimate(Location location);

  Weather getFutureClimate(Location location, LocalDateTime dateTime);
}




