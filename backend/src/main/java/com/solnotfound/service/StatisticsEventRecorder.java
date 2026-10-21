package com.solnotfound.service;

import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.repository.IStatisticsEventRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StatisticsEventRecorder {
  public static final String OPEN_METEO = "Open-Meteo";

  private final IStatisticsEventRepository repository;
  private final Clock clock;

  @Autowired
  public StatisticsEventRecorder(IStatisticsEventRepository repository) {
    this(repository, Clock.systemUTC());
  }

  StatisticsEventRecorder(IStatisticsEventRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Records an activity event after the corresponding domain state has been persisted. */
  public void recordActivity(
      StatisticsEventType type, String activityId, ActivityTransitionReason reason) {
    repository.save(
        new StatisticsEvent(null, type, Instant.now(clock), activityId, reason, null, null));
  }

  /** Records the outcome and elapsed time of one real request to Open-Meteo. */
  public void recordWeatherRequest(boolean successful, long durationMs) {
    StatisticsEventType type =
        successful
            ? StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED
            : StatisticsEventType.WEATHER_PROVIDER_FAILED;
    repository.save(
        new StatisticsEvent(null, type, Instant.now(clock), null, null, durationMs, OPEN_METEO));
  }

  /** Records one attempt to call a weather provider; {@code failure} is null when it succeeded. */
  public void recordProviderCall(
      String provider, boolean successful, long durationMs, String failure) {
    StatisticsEventType type =
        successful
            ? StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED
            : StatisticsEventType.WEATHER_PROVIDER_FAILED;
    repository.save(event(type, provider, failure, durationMs, null, null, null));
  }

  /** Records a call to a provider that a protection refused before reaching it. */
  public void recordProviderRefusal(String provider, String reason) {
    repository.save(
        event(
            StatisticsEventType.WEATHER_PROVIDER_REFUSED,
            provider,
            reason,
            null,
            null,
            null,
            null));
  }

  /** Records a lookup that had to be answered by the main provider instead of {@code provider}. */
  public void recordFallback(String provider) {
    repository.save(
        event(
            StatisticsEventType.WEATHER_PROVIDER_FALLBACK, provider, null, null, null, null, null));
  }

  /** Records a provider's status, breaker state and cache counters since the previous snapshot. */
  public void recordProviderSnapshot(
      String provider,
      String status,
      String circuitBreakerState,
      long cacheHits,
      long cacheMisses) {
    repository.save(
        event(
            StatisticsEventType.WEATHER_PROVIDER_SNAPSHOT,
            provider,
            status,
            null,
            cacheHits,
            cacheMisses,
            circuitBreakerState));
  }

  private StatisticsEvent event(
      StatisticsEventType type,
      String provider,
      String outcome,
      Long durationMs,
      Long cacheHits,
      Long cacheMisses,
      String circuitBreakerState) {
    return new StatisticsEvent(
        null,
        type,
        Instant.now(clock),
        null,
        null,
        durationMs,
        provider,
        outcome,
        cacheHits,
        cacheMisses,
        circuitBreakerState);
  }
}
