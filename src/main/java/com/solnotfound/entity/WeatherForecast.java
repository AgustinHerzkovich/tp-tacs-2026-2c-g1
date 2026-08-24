package com.solnotfound.entity;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class WeatherForecast {
  private Integer id;
  private LocalDateTime datetime;
  private Float temperature;
  private Float chanceOfRain;
  private Float windSpeed;
}
