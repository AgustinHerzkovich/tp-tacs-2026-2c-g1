package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityType;
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
    List<UserDTO> participants,
    /**
     * IANA zone the activity's dates are written in (e.g. {@code America/Argentina/Buenos_Aires}),
     * or {@code null} when it was never recorded. See {@link ActivityResponse#timeZone()}.
     */
    String timeZone) {

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
