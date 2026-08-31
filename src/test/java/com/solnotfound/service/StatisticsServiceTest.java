package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.dto.StatisticsResponse;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.exception.InvalidStatisticsRangeException;
import com.solnotfound.repository.InMemoryStatisticsEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");

  private InMemoryStatisticsEventRepository repository;
  private StatisticsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryStatisticsEventRepository();
    service = new StatisticsService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void aggregatesActivityAndWeatherEvents() {
    save(StatisticsEventType.ACTIVITY_CREATED, null, null);
    save(
        StatisticsEventType.ACTIVITY_RESCHEDULED, ActivityTransitionReason.VOTATION_RESOLVED, null);
    save(StatisticsEventType.ACTIVITY_CANCELLED, ActivityTransitionReason.BAD_WEATHER, null);
    save(StatisticsEventType.ACTIVITY_CANCELLED, ActivityTransitionReason.QUORUM_NOT_REACHED, null);
    save(StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED, null, 100L);
    save(StatisticsEventType.WEATHER_PROVIDER_FAILED, null, 300L);

    StatisticsResponse response = service.getStatistics(NOW.minusSeconds(1), NOW.plusSeconds(1));

    assertThat(response.activities().created()).isEqualTo(1);
    assertThat(response.activities().rescheduled()).isEqualTo(1);
    assertThat(response.activities().cancelled()).isEqualTo(2);
    assertThat(response.activities().cancelledByWeather()).isEqualTo(1);
    assertThat(response.weatherProvider().requests()).isEqualTo(2);
    assertThat(response.weatherProvider().successful()).isEqualTo(1);
    assertThat(response.weatherProvider().failed()).isEqualTo(1);
    assertThat(response.weatherProvider().averageResponseTimeMs()).isEqualTo(200.0);
  }

  @Test
  void defaultsToLastSevenDaysAndReturnsZerosWithoutEvents() {
    StatisticsResponse response = service.getStatistics(null, null);

    assertThat(response.from()).isEqualTo(NOW.minusSeconds(7 * 24 * 60 * 60));
    assertThat(response.to()).isEqualTo(NOW);
    assertThat(response.activities().created()).isZero();
    assertThat(response.weatherProvider().requests()).isZero();
    assertThat(response.weatherProvider().averageResponseTimeMs()).isZero();
  }

  @Test
  void rejectsInvertedRange() {
    assertThatThrownBy(() -> service.getStatistics(NOW, NOW.minusSeconds(1)))
        .isInstanceOf(InvalidStatisticsRangeException.class);
  }

  private void save(
      StatisticsEventType type, ActivityTransitionReason reason, Long durationMilliseconds) {
    repository.save(
        new StatisticsEvent(
            null, type, NOW, "activity-1", reason, durationMilliseconds, "Open-Meteo"));
  }
}
