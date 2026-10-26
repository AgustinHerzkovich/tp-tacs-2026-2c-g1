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
    List<String> imageUrls,
    String organizerId,
    /**
     * IANA zone the activity's dates are written in (e.g. {@code America/Argentina/Buenos_Aires}),
     * or {@code null} when it was never recorded. Clients that need to know whether an already-past
     * date is really past need it to resolve {@link #dateTime} against their own clock.
     */
    String timeZone) {

  public ActivityResponse {
    participants = List.copyOf(participants);
    imageUrls = List.copyOf(imageUrls);
  }
}
