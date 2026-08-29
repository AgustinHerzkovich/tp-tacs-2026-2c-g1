package com.solnotfound.dto;

import com.solnotfound.entity.ActivityStatus;

public record ActivityStatisticsResponse(ActivityStatus type, long totalActivities) {}
