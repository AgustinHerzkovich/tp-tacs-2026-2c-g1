package com.solnotfound.dto;

import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.ActivityType;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityResponse(
    String id,
    String title,
    String description,
    ActivityType type,
    LocationDTO location,
    LocalDateTime dateTime,
    Boolean availability,
    Integer minParticipants,
    Integer maxParticipants,
    Integer participantCount,
    List<ParticipantDTO> participants,
    WeatherConditionsDTO weatherConditions,
    Integer anticipationWindow,
    ReprogramationRangeDTO reprogramationRange,
    ActivityStatus status) {

  public ActivityResponse {
    participants = List.copyOf(participants);
  }
}
