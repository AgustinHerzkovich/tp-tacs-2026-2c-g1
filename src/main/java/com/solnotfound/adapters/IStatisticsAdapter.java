package com.solnotfound.adapters;

import com.solnotfound.entity.ActivityStatistics;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.WeatherForecastStatistics;
import java.time.LocalDate;

public interface IStatisticsAdapter {

  WeatherForecastStatistics getWeatherForecastServiceStatistics(
      LocalDate startDate, LocalDate endDate);

  ActivityStatistics getActivityStatistics(ActivityStatus activityStatus);
}
