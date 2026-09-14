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
    // Not @Future: this is a naive local date-time (the organizer's wall-clock
    // reading, also used as-is for weather matching), so "is it in the future"
    // can only be judged against the organizer's own time zone, not the
    // server's. See ActivityService#validateFutureDateTime.
    @NotNull LocalDateTime dateTime,
    @NotNull @Min(1) Integer minParticipants,
    @NotNull @Min(1) Integer maxParticipants,
    @NotNull @Valid WeatherConditionsDTO weatherConditions,
    @NotNull @PositiveOrZero Integer anticipationWindow,
    @NotNull @Valid ReprogramationRangeDTO reprogramationRange) {}
