package com.solnotfound.dto;

public record WeatherForecastStatisticsResponse(
    Integer totalRequests,
    Integer failedRequests,
    Integer successfulRequests,
    Double averageResponseTime) {}
