package com.solnotfound.dto;

import com.solnotfound.entity.activity.ActivityStatus;

public record ActivityStatisticsResponse(ActivityStatus type, long totalActivities) {}
