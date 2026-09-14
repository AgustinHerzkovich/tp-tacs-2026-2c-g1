package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@DataMongoTest
@Testcontainers
class StatisticsEventRepositoryTest {
  private static final Instant FROM = Instant.parse("2026-09-01T10:00:00Z");
  private static final Instant TO = Instant.parse("2026-09-01T11:00:00Z");

  @Container @ServiceConnection
  static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0.14");

  @Autowired private IStatisticsEventRepository repository;

  @BeforeEach
  void clearEvents() {
    repository.deleteAll();
  }

  @Test
  void persistsEventsAndIncludesBothRangeBoundaries() {
    repository.save(event(FROM, StatisticsEventType.ACTIVITY_CREATED));
    repository.save(event(TO, StatisticsEventType.ACTIVITY_CANCELLED));
    repository.save(event(FROM.minusMillis(1), StatisticsEventType.ACTIVITY_RESCHEDULED));
    repository.save(event(TO.plusMillis(1), StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED));

    assertThat(repository.findOccurredBetween(FROM, TO))
        .extracting(StatisticsEvent::type)
        .containsExactlyInAnyOrder(
            StatisticsEventType.ACTIVITY_CREATED, StatisticsEventType.ACTIVITY_CANCELLED);
  }

  private StatisticsEvent event(Instant occurredAt, StatisticsEventType type) {
    return new StatisticsEvent(
        null, type, occurredAt, "activity-1", ActivityTransitionReason.BAD_WEATHER, null, null);
  }
}
