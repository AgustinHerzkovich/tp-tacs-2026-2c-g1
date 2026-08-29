package com.solnotfound.dto;

public record WeatherForecastStatisticsResponse(
    Integer TotalRequests,
    Integer FailedRequests,
    Integer SuccessfulRequests,
    Double AverageResponseTime) {}
