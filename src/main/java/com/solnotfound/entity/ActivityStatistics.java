package com.solnotfound.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ActivityStatistics {
  private Long totalActivities;
  private ActivityStatus activityStatus;
}
