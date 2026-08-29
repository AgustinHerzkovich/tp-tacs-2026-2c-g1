package com.solnotfound.adapters;

import com.solnotfound.entity.ActivityStatistics;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.WeatherForecastStatistics;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class InMemoryStatisticsAdapter implements IStatisticsAdapter {
  @Override
  public WeatherForecastStatistics getWeatherForecastServiceStatistics(LocalDate startDate, LocalDate endDate) {
    if(startDate.isAfter(LocalDate.now())) {
      System.out.println("Start date is in the future. Returning empty statistics.");
    }else{
      System.out.println("Returning statistics for the period: " + startDate + " to " + endDate);
    }
    if(endDate.isAfter(LocalDate.now())) {
      System.out.println("fuck off");
    }
    return new WeatherForecastStatistics(100, 80, 20, 200.0);
  }

  @Override
  public ActivityStatistics getActivityStatistics(ActivityStatus activityStatus) {
    return new ActivityStatistics(50L, activityStatus);
  }
}
