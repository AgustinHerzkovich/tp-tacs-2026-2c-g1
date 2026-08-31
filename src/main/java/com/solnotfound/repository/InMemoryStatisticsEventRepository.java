package com.solnotfound.repository;

import com.solnotfound.entity.statistics.StatisticsEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStatisticsEventRepository implements IStatisticsEventRepository {
  private final Map<String, StatisticsEvent> events = new ConcurrentHashMap<>();

  @Override
  public StatisticsEvent save(StatisticsEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    Objects.requireNonNull(event.type(), "event type must not be null");
    Objects.requireNonNull(event.occurredAt(), "event occurrence time must not be null");
    String id = event.id() == null ? UUID.randomUUID().toString() : event.id();
    StatisticsEvent persisted =
        new StatisticsEvent(
            id,
            event.type(),
            event.occurredAt(),
            event.activityId(),
            event.reason(),
            event.durationMs(),
            event.provider());
    events.put(id, persisted);
    return persisted;
  }

  @Override
  public List<StatisticsEvent> findOccurredBetween(Instant from, Instant to) {
    return events.values().stream()
        .filter(event -> !event.occurredAt().isBefore(from) && !event.occurredAt().isAfter(to))
        .toList();
  }

  @Override
  public void deleteAll() {
    events.clear();
  }
}
