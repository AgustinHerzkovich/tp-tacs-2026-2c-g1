package com.solnotfound.service;

import com.solnotfound.adapters.IStatisticsAdapter;
import com.solnotfound.dto.ActivityStatisticsResponse;
import com.solnotfound.dto.WeatherForecastStatisticsResponse;
import com.solnotfound.entity.ActivityStatistics;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.WeatherForecastStatistics;
import com.solnotfound.exception.CouldNotRetrieveStatisticsException;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import com.solnotfound.exception.CouldNotRetrieveStatisticsException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {
  private IStatisticsAdapter statisticsAdapter;

  public StatisticsService(IStatisticsAdapter statisticsAdapter) {
    this.statisticsAdapter = statisticsAdapter;
  }

  public WeatherForecastStatisticsResponse getWeatherForecastServiceStatistics(
      LocalDate startDate, LocalDate endDate) {
    try {
      WeatherForecastStatistics wfstatistics =
          statisticsAdapter.getWeatherForecastServiceStatistics(startDate, endDate);
      return toResponse(wfstatistics);
    } catch (Exception e) {
      // Handle the exception, log it, or rethrow it as a custom exception
      throw new CouldNotRetrieveStatisticsException(
          "Could not retrieve weather forecast service statistics", e);
    }
  }

  private WeatherForecastStatisticsResponse toResponse(WeatherForecastStatistics wfstatistics) {
    return new WeatherForecastStatisticsResponse(
        wfstatistics.getTotalRequests(),
        wfstatistics.getSuccessfulRequests(),
        wfstatistics.getFailedRequests(),
        wfstatistics.getAverageResponseTime());
  }

  private ActivityStatisticsResponse toResponse(ActivityStatistics activityStatistics) {
    return new ActivityStatisticsResponse(
        activityStatistics.getActivityStatus(), activityStatistics.getTotalActivities());
  }

  public ActivityStatisticsResponse getActivityStatistics(ActivityStatus activityStatus) {
    try {
      ActivityStatistics activityStatistics =
          statisticsAdapter.getActivityStatistics(activityStatus);
      return toResponse(activityStatistics);
    } catch (Exception e) {
      // Handle the exception, log it, or rethrow it as a custom exception
      throw new CouldNotRetrieveStatisticsException("Could not retrieve activity statistics", e);
    }
  }



}
