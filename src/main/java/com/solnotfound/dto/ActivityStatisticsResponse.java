package com.solnotfound.dto;

import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.ActivityType;

public record ActivityStatisticsResponse(
  ActivityStatus type,
  long totalActivities
) {
}
