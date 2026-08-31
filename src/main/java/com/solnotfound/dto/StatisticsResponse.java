package com.solnotfound.dto;

import java.time.Instant;

public record StatisticsResponse(
    Instant from,
    Instant to,
    ActivityStatisticsResponse activities,
    WeatherProviderStatisticsResponse weatherProvider) {}
