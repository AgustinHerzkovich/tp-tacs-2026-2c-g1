package com.solnotfound.service;

import com.solnotfound.adapters.IStatisticsAdapter;
import com.solnotfound.dto.WeatherForecastStatisticsResponse;
import com.solnotfound.entity.WeatherForecastStatistics;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class StatisticsService {
  private IStatisticsAdapter statisticsAdapter;
  public StatisticsService(IStatisticsAdapter statisticsAdapter) {this.statisticsAdapter = statisticsAdapter;}
  public WeatherForecastStatisticsResponse getWeatherForecastServiceStatistics(LocalDate startDate, LocalDate endDate) {
    WeatherForecastStatistics wfstatistics = statisticsAdapter.getWeatherForecastServiceStatistics(startDate, endDate);
    return toResponse(wfstatistics);
  }

  private WeatherForecastStatisticsResponse toResponse(WeatherForecastStatistics wfstatistics) {
    return new WeatherForecastStatisticsResponse(
      wfstatistics.getTotalRequests(),
      wfstatistics.getSuccessfulRequests(),
      wfstatistics.getFailedRequests(),
      wfstatistics.getAverageResponseTime()
    );
  }
}
