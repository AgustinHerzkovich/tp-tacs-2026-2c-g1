package com.solnotfound.entity.statistics;

import java.time.Instant;

public record StatisticsEvent(
    String id,
    StatisticsEventType type,
    Instant occurredAt,
    String activityId,
    ActivityTransitionReason reason,
    Long durationMs,
    String provider) {}
