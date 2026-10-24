package com.solnotfound.entity.statistics;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An immutable fact used by the administrator statistics.
 *
 * @param outcome failure kind of a provider call, reason a call was refused, or provider status of
 *     a snapshot; {@code null} for activity events and successful calls
 * @param cacheHits cache hits since the previous snapshot of the provider
 * @param cacheMisses cache misses since the previous snapshot of the provider
 * @param circuitBreakerState state of the provider's circuit breaker when the snapshot was taken
 * @param occurrences how many facts the event stands for: refusals and fallbacks are counted in
 *     memory and stored as one event per kind; {@code null} means a single one
 */
@Document(collection = "statistics_events")
public record StatisticsEvent(
    @Id String id,
    StatisticsEventType type,
    @Indexed Instant occurredAt,
    String activityId,
    ActivityTransitionReason reason,
    Long durationMs,
    String provider,
    String outcome,
    Long cacheHits,
    Long cacheMisses,
    String circuitBreakerState,
    Long occurrences) {

  public StatisticsEvent(
      String id,
      StatisticsEventType type,
      Instant occurredAt,
      String activityId,
      ActivityTransitionReason reason,
      Long durationMs,
      String provider) {
    this(
        id,
        type,
        occurredAt,
        activityId,
        reason,
        durationMs,
        provider,
        null,
        null,
        null,
        null,
        null);
  }
}
