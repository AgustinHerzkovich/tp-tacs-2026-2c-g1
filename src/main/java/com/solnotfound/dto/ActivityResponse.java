package com.solnotfound.dto;

import com.solnotfound.entity.ActivityType;
import java.time.LocalDateTime;

public record ActivityResponse(
    String id,
    String title,
    String description,
    ActivityType type,
    LocationDTO location,
    LocalDateTime dateTime,
    Integer minParticipants,
    Integer maxParticipants,
    WeatherConditionsDTO weatherConditions,
    Integer anticipationWindow) {}
