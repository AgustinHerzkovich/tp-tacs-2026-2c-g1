package com.solnotfound.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record WeatherConditionsDTO(
    @Min(0) @Max(100) Integer maxRainProbability,
    Integer minTemperature,
    Integer maxTemperature,
    @PositiveOrZero Double maxWindSpeed) {}
