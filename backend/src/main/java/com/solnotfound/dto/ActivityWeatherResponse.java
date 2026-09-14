package com.solnotfound.dto;

import java.time.LocalDateTime;

public record ActivityWeatherResponse(
    String activityId,
    LocationDTO location,
    LocalDateTime activityDateTime,
    WeatherForecastDTO currentWeather,
    WeatherForecastDTO activityForecast) {}
