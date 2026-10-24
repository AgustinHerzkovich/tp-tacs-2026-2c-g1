package com.solnotfound.dto;

import com.solnotfound.entity.user.WeatherProvider;
import java.time.LocalDateTime;

/**
 * Current weather and activity-time forecast of an activity.
 *
 * @param fallback {@code true} when the user chose the legacy provider but it could not answer, so
 *     the main provider did
 */
public record ActivityWeatherResponse(
    String activityId,
    LocationDTO location,
    LocalDateTime activityDateTime,
    WeatherForecastDTO currentWeather,
    WeatherForecastDTO activityForecast,
    WeatherProvider provider,
    boolean fallback) {}
