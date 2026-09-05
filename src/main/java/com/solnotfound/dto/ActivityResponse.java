package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
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
    ActivityStatus status,
    List<String> imageUrls) {

  public ActivityResponse {
    participants = List.copyOf(participants);
    imageUrls = List.copyOf(imageUrls);
  }
}
