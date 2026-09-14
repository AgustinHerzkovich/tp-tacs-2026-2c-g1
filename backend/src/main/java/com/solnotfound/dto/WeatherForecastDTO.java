package com.solnotfound.dto;

import java.time.LocalDateTime;

public record WeatherForecastDTO(
    LocalDateTime dateTime, Float temperature, Float chanceOfRain, Float windSpeed) {}
