package com.solnotfound.adapters;

import com.solnotfound.entity.activity.ActivityStatistics;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.weather.WeatherForecastStatistics;
import java.time.LocalDate;

public interface IStatisticsAdapter {

  WeatherForecastStatistics getWeatherForecastServiceStatistics(
      LocalDate startDate, LocalDate endDate);

  ActivityStatistics getActivityStatistics(ActivityStatus activityStatus);
}
