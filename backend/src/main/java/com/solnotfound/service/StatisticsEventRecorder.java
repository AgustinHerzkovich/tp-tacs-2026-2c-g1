package com.solnotfound.service;

import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.repository.IStatisticsEventRepository;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Stores statistics events.
 *
 * <p>Events about weather providers are recorded in the middle of a weather lookup, often right
 * when a provider failed and the fallback is about to answer. They are best effort: a failure to
 * store one is logged and swallowed, so statistics can never take the weather down with them.
 *
 * <p>Refusals and fallbacks happen once per user lookup and are not bounded by any protection: with
 * the legacy provider down, every lookup produces both. They are counted in memory and stored by
 * {@link #flushProviderCounters()}, one event per kind with its count, so the fallback path never
 * writes to the database.
 */
@Slf4j
@Component
public class StatisticsEventRecorder {
  public static final String OPEN_METEO = "Open-Meteo";

  private final IStatisticsEventRepository repository;
  private final Clock clock;
  private final ConcurrentMap<CounterKey, AtomicLong> pendingCounters = new ConcurrentHashMap<>();

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
    saveProviderEvent(
        new StatisticsEvent(null, type, Instant.now(clock), null, null, durationMs, OPEN_METEO));
  }

  /** Records one attempt to call a weather provider; {@code failure} is null when it succeeded. */
  public void recordProviderCall(
      String provider, boolean successful, long durationMs, String failure) {
    StatisticsEventType type =
        successful
            ? StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED
            : StatisticsEventType.WEATHER_PROVIDER_FAILED;
    saveProviderEvent(event(type, provider, failure, durationMs, null, null, null));
  }

  /** Counts a call to a provider that a protection refused before reaching it. */
  public void recordProviderRefusal(String provider, String reason) {
    count(StatisticsEventType.WEATHER_PROVIDER_REFUSED, provider, reason);
  }

  /** Counts a lookup that had to be answered by the main provider instead of {@code provider}. */
  public void recordFallback(String provider) {
    count(StatisticsEventType.WEATHER_PROVIDER_FALLBACK, provider, null);
  }

  /**
   * Stores the refusals and fallbacks counted since the previous flush, one event per type,
   * provider and reason carrying its count, timed at the flush. Called with every provider snapshot
   * and when the instance shuts down. A count that cannot be stored is kept for the next flush.
   */
  @PreDestroy
  public void flushProviderCounters() {
    pendingCounters.forEach(
        (key, counter) -> {
          long occurrences = counter.getAndSet(0);
          if (occurrences == 0) {
            return;
          }
          try {
            repository.save(
                new StatisticsEvent(
                    null,
                    key.type(),
                    Instant.now(clock),
                    null,
                    null,
                    null,
                    key.provider(),
                    key.outcome(),
                    null,
                    null,
                    null,
                    occurrences));
          } catch (RuntimeException exception) {
            counter.addAndGet(occurrences);
            log.warn(
                "Could not record weather provider counters: type={} provider={}",
                key.type(),
                key.provider(),
                exception);
          }
        });
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

  private void saveProviderEvent(StatisticsEvent event) {
    try {
      repository.save(event);
    } catch (RuntimeException exception) {
      log.warn(
          "Could not record weather provider event: type={} provider={}",
          event.type(),
          event.provider(),
          exception);
    }
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
        circuitBreakerState,
        null);
  }

  private void count(StatisticsEventType type, String provider, String outcome) {
    pendingCounters
        .computeIfAbsent(new CounterKey(type, provider, outcome), key -> new AtomicLong())
        .incrementAndGet();
  }

  private record CounterKey(StatisticsEventType type, String provider, String outcome) {}
}
