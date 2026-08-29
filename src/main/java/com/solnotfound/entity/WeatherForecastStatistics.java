package com.solnotfound.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class WeatherForecastStatistics {
  private Integer totalRequests;
  private Integer successfulRequests;
  private Integer failedRequests;
  private Double averageResponseTime;
}
