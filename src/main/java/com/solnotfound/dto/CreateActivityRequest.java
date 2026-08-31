package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateActivityRequest(
    @NotBlank String title,
    String description,
    @NotNull ActivityType type,
    @NotNull @Valid LocationDTO location,
    @NotNull @Future LocalDateTime dateTime,
    @NotNull @Min(1) Integer minParticipants,
    @NotNull @Min(1) Integer maxParticipants,
    @NotNull @Valid WeatherConditionsDTO weatherConditions,
    @NotNull @PositiveOrZero Integer anticipationWindow,
    @NotNull @Valid ReprogramationRangeDTO reprogramationRange) {}
