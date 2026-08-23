package com.solnotfound.entity;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class Weather {
  private Integer id;
  private LocalDateTime datetime;
  private Float temprature;
  private Float chanceOfRain;
  private Float windSpeed;
}
