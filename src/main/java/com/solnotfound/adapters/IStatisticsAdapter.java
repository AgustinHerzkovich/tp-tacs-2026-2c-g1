package com.solnotfound.adapters;

import com.solnotfound.entity.WeatherForecastStatistics;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IStatisticsAdapter {

  WeatherForecastStatistics getWeatherForecastServiceStatistics(LocalDate startDate, LocalDate endDate);
}
