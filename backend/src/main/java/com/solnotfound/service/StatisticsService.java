package com.solnotfound.service;

import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.dto.ActivityStatisticsResponse;
import com.solnotfound.dto.ProviderStatisticsResponse;
import com.solnotfound.dto.ProviderStatusPoint;
import com.solnotfound.dto.StatisticsResponse;
import com.solnotfound.dto.WeatherProviderStatisticsResponse;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.ProviderStatus;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.exception.InvalidStatisticsRangeException;
import com.solnotfound.repository.IStatisticsEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {
  private static final Duration DEFAULT_RANGE = Duration.ofDays(7);
  private static final int MAX_TIMELINE_POINTS = 120;
  private static final List<String> PROVIDERS =
      List.of(StatisticsEventRecorder.OPEN_METEO, LegacyWeatherProvider.PROVIDER);
  private static final Set<ActivityTransitionReason> WEATHER_CANCELLATION_REASONS =
      Set.of(
          ActivityTransitionReason.BAD_WEATHER, ActivityTransitionReason.NO_WEATHER_ALTERNATIVES);

  private final IStatisticsEventRepository repository;
  private final Clock clock;

  @Autowired
  public StatisticsService(IStatisticsEventRepository repository) {
    this(repository, Clock.systemUTC());
  }

  StatisticsService(IStatisticsEventRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Aggregates activity and provider events within an inclusive time range. Missing boundaries
   * default to the last seven days ending at the current instant.
   *
   * @param requestedFrom optional inclusive lower boundary
   * @param requestedTo optional inclusive upper boundary
   * @return statistics calculated from persisted events
   * @throws InvalidStatisticsRangeException when the lower boundary is after the upper boundary
   */
  public StatisticsResponse getStatistics(Instant requestedFrom, Instant requestedTo) {
    Instant to = requestedTo == null ? Instant.now(clock) : requestedTo;
    Instant from = requestedFrom == null ? to.minus(DEFAULT_RANGE) : requestedFrom;
    if (from.isAfter(to)) {
      throw new InvalidStatisticsRangeException("Statistics start date cannot be after end date");
    }

    List<StatisticsEvent> events = repository.findOccurredBetween(from, to);
    long created = count(events, StatisticsEventType.ACTIVITY_CREATED);
    long rescheduled = count(events, StatisticsEventType.ACTIVITY_RESCHEDULED);
    long cancelled = count(events, StatisticsEventType.ACTIVITY_CANCELLED);
    long cancelledByWeather =
        events.stream()
            .filter(event -> event.type() == StatisticsEventType.ACTIVITY_CANCELLED)
            .filter(event -> WEATHER_CANCELLATION_REASONS.contains(event.reason()))
            .count();

    List<StatisticsEvent> openMeteo =
        events.stream()
            .filter(event -> isProvider(event, StatisticsEventRecorder.OPEN_METEO))
            .toList();
    long successful = count(openMeteo, StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED);
    long failed = count(openMeteo, StatisticsEventType.WEATHER_PROVIDER_FAILED);

    return new StatisticsResponse(
        from,
        to,
        new ActivityStatisticsResponse(created, rescheduled, cancelled, cancelledByWeather),
        new WeatherProviderStatisticsResponse(
            successful + failed, successful, failed, averageDuration(openMeteo)),
        PROVIDERS.stream().map(provider -> providerStatistics(provider, events)).toList());
  }

  /**
   * Aggregates the events of one weather provider. Calls and failures come from one event each;
   * refusals and fallbacks from the counts stored with each snapshot; cache counters and the status
   * timeline come from the periodic snapshots taken by {@link WeatherProviderMonitor}. The timeline
   * keeps at most {@value #MAX_TIMELINE_POINTS} points: when there are more, each point summarizes
   * a stretch of time with the worst status seen in it, so a short outage is never averaged away.
   */
  private ProviderStatisticsResponse providerStatistics(
      String provider, List<StatisticsEvent> events) {
    List<StatisticsEvent> own =
        events.stream().filter(event -> isProvider(event, provider)).toList();
    long successful = count(own, StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED);
    long failed = count(own, StatisticsEventType.WEATHER_PROVIDER_FAILED);
    List<StatisticsEvent> snapshots =
        own.stream()
            .filter(event -> event.type() == StatisticsEventType.WEATHER_PROVIDER_SNAPSHOT)
            .sorted(Comparator.comparing(StatisticsEvent::occurredAt))
            .toList();
    long hits = snapshots.stream().mapToLong(event -> orZero(event.cacheHits())).sum();
    long misses = snapshots.stream().mapToLong(event -> orZero(event.cacheMisses())).sum();
    StatisticsEvent latest = snapshots.isEmpty() ? null : snapshots.getLast();
    return new ProviderStatisticsResponse(
        provider,
        latest == null ? null : latest.outcome(),
        latest == null ? null : latest.circuitBreakerState(),
        successful + failed,
        successful,
        failed,
        averageDuration(own),
        hits,
        misses,
        hits + misses == 0 ? null : (double) hits / (hits + misses),
        count(own, StatisticsEventType.WEATHER_PROVIDER_FALLBACK),
        countByOutcome(own, StatisticsEventType.WEATHER_PROVIDER_FAILED, "ERROR"),
        countByOutcome(own, StatisticsEventType.WEATHER_PROVIDER_REFUSED, "REFUSED"),
        timeline(snapshots));
  }

  private static List<ProviderStatusPoint> timeline(List<StatisticsEvent> snapshots) {
    int bucketSize = Math.max(1, (int) Math.ceil(snapshots.size() / (double) MAX_TIMELINE_POINTS));
    List<ProviderStatusPoint> points = new ArrayList<>();
    for (int start = 0; start < snapshots.size(); start += bucketSize) {
      List<StatisticsEvent> bucket =
          snapshots.subList(start, Math.min(snapshots.size(), start + bucketSize));
      StatisticsEvent worst =
          bucket.stream().max(Comparator.comparingInt(StatisticsService::severity)).orElseThrow();
      points.add(
          new ProviderStatusPoint(
              bucket.getFirst().occurredAt(), worst.outcome(), worst.circuitBreakerState()));
    }
    return points;
  }

  private static int severity(StatisticsEvent snapshot) {
    for (ProviderStatus status : ProviderStatus.values()) {
      if (status.name().equals(snapshot.outcome())) {
        return status.ordinal();
      }
    }
    return -1;
  }

  private static Map<String, Long> countByOutcome(
      List<StatisticsEvent> events, StatisticsEventType type, String unknown) {
    return events.stream()
        .filter(event -> event.type() == type)
        .collect(
            Collectors.groupingBy(
                event -> event.outcome() == null ? unknown : event.outcome(),
                TreeMap::new,
                Collectors.summingLong(StatisticsService::occurrences)));
  }

  private double averageDuration(List<StatisticsEvent> events) {
    return events.stream()
        .filter(this::isWeatherRequest)
        .map(StatisticsEvent::durationMs)
        .filter(duration -> duration != null)
        .mapToLong(Long::longValue)
        .average()
        .orElse(0.0);
  }

  /** Open-Meteo events recorded before providers were named carry no provider. */
  private static boolean isProvider(StatisticsEvent event, String provider) {
    String recorded =
        event.provider() == null ? StatisticsEventRecorder.OPEN_METEO : event.provider();
    return provider.equals(recorded);
  }

  private static long orZero(Long value) {
    return value == null ? 0 : value;
  }

  private long count(List<StatisticsEvent> events, StatisticsEventType type) {
    return events.stream()
        .filter(event -> event.type() == type)
        .mapToLong(StatisticsService::occurrences)
        .sum();
  }

  /** Refusals and fallbacks are stored as counts; every other event stands for a single one. */
  private static long occurrences(StatisticsEvent event) {
    return event.occurrences() == null ? 1 : event.occurrences();
  }

  private boolean isWeatherRequest(StatisticsEvent event) {
    return event.type() == StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED
        || event.type() == StatisticsEventType.WEATHER_PROVIDER_FAILED;
  }
}
