package com.solnotfound.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherForecast {
  private Integer id;
  private LocalDateTime dateTime;
  private Float temperature;
  private Float chanceOfRain;
  private Float windSpeed;
}
