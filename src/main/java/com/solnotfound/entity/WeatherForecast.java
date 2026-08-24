package com.solnotfound.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WeatherForecast {
  private Integer id;
  private LocalDateTime datetime;
  private Float temperature;
  private Float chanceOfRain;
  private Float windSpeed;
}
