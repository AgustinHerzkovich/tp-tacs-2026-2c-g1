package com.solnotfound.dto;

import com.solnotfound.entity.ActivityType;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityDTO(
    String id,
    String title,
    String description,
    ActivityType type,
    LocationDTO location,
    LocalDateTime dateTime,
    Boolean availability,
    Integer minParticipants,
    Integer maxParticipants,
    WeatherConditionsDTO weatherConditions,
    Integer anticipationWindow,
    ReprogramationRangeDTO reprogramationRange,
    UserDTO organizer,
    List<UserDTO> participants) {

  public ActivityDTO {
    if (participants != null) {
      participants = List.copyOf(participants);
    }
  }

  @Override
  public List<UserDTO> participants() {
    if (participants == null) {
      return null;
    }

    return List.copyOf(participants);
  }
}
