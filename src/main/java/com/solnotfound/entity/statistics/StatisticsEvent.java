package com.solnotfound.entity.statistics;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "statistics_events")
public record StatisticsEvent(
    @Id String id,
    StatisticsEventType type,
    Instant occurredAt,
    String activityId,
    ActivityTransitionReason reason,
    Long durationMs,
    String provider) {}
