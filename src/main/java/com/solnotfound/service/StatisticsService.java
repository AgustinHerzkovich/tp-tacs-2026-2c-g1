package com.solnotfound.service;

import com.solnotfound.dto.ActivityStatisticsResponse;
import com.solnotfound.dto.StatisticsResponse;
import com.solnotfound.dto.WeatherProviderStatisticsResponse;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.exception.InvalidStatisticsRangeException;
import com.solnotfound.repository.IStatisticsEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {
  private static final Duration DEFAULT_RANGE = Duration.ofDays(7);
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

    long successful = count(events, StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED);
    long failed = count(events, StatisticsEventType.WEATHER_PROVIDER_FAILED);
    double averageResponseTimeMs =
        events.stream()
            .filter(this::isWeatherRequest)
            .map(StatisticsEvent::durationMs)
            .filter(duration -> duration != null)
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

    return new StatisticsResponse(
        from,
        to,
        new ActivityStatisticsResponse(created, rescheduled, cancelled, cancelledByWeather),
        new WeatherProviderStatisticsResponse(
            successful + failed, successful, failed, averageResponseTimeMs));
  }

  private long count(List<StatisticsEvent> events, StatisticsEventType type) {
    return events.stream().filter(event -> event.type() == type).count();
  }

  private boolean isWeatherRequest(StatisticsEvent event) {
    return event.type() == StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED
        || event.type() == StatisticsEventType.WEATHER_PROVIDER_FAILED;
  }
}
