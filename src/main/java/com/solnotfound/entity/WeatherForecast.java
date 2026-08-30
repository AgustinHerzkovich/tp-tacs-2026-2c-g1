package com.solnotfound.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherForecast {
  private final LocalDateTime dateTime;
  private final Float temperature;
  private final Float chanceOfRain;
  private final Float windSpeed;
}
