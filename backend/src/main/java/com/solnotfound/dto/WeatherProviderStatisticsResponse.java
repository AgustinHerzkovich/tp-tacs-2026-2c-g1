package com.solnotfound.dto;

public record WeatherProviderStatisticsResponse(
    long requests, long successful, long failed, double averageResponseTimeMs) {}
