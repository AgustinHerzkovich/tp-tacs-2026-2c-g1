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
  private static final String OPEN_METEO = "Open-Meteo";

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
}
