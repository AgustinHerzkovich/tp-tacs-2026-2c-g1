package com.solnotfound.dto;

public record ActivityStatisticsResponse(
    long created, long rescheduled, long cancelled, long cancelledByWeather) {}
