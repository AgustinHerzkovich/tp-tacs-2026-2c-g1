package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityFilterDTO(
    ActivityType type,
    String city,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    Boolean availability,
    List<ActivityStatus> statuses) {

  public ActivityFilterDTO(
      ActivityType type,
      String city,
      LocalDateTime dateFrom,
      LocalDateTime dateTo,
      Boolean availability) {
    this(type, city, dateFrom, dateTo, availability, List.of());
  }

  public ActivityFilterDTO {
    statuses = statuses == null ? List.of() : List.copyOf(statuses);
  }
}
