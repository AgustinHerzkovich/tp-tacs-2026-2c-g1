package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryStatisticsEventRepositoryTest {

  @Test
  void returnsOnlyEventsWithinInclusiveRange() {
    InMemoryStatisticsEventRepository repository = new InMemoryStatisticsEventRepository();
    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-02T00:00:00Z");
    repository.save(event(from.minusNanos(1)));
    repository.save(event(from));
    repository.save(event(to));
    repository.save(event(to.plusNanos(1)));

    assertThat(repository.findOccurredBetween(from, to))
        .extracting(StatisticsEvent::occurredAt)
        .containsExactlyInAnyOrder(from, to);
  }

  @Test
  void generatesIdentifierAndCanDeleteAllEvents() {
    InMemoryStatisticsEventRepository repository = new InMemoryStatisticsEventRepository();
    StatisticsEvent saved = repository.save(event(Instant.now()));

    assertThat(saved.id()).isNotBlank();

    repository.deleteAll();

    assertThat(repository.findOccurredBetween(Instant.EPOCH, Instant.MAX)).isEmpty();
  }

  private StatisticsEvent event(Instant occurredAt) {
    return new StatisticsEvent(
        null, StatisticsEventType.ACTIVITY_CREATED, occurredAt, "activity-1", null, null, null);
  }
}
