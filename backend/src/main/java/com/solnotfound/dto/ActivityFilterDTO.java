package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Optional criteria for searching activities; {@code null} or empty values are ignored.
 *
 * @param title case-insensitive text that the activity title must contain
 */
public record ActivityFilterDTO(
    ActivityType type,
    String city,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    Boolean availability,
    List<ActivityStatus> statuses,
    String title) {

  public ActivityFilterDTO(
      ActivityType type,
      String city,
      LocalDateTime dateFrom,
      LocalDateTime dateTo,
      Boolean availability) {
    this(type, city, dateFrom, dateTo, availability, List.of(), null);
  }

  public ActivityFilterDTO {
    statuses = statuses == null ? List.of() : List.copyOf(statuses);
  }
}
