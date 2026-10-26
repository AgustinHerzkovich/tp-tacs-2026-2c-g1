package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.dto.StatisticsResponse;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.exception.InvalidStatisticsRangeException;
import com.solnotfound.repository.IStatisticsEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");

  private IStatisticsEventRepository repository;
  private StatisticsService service;

  @BeforeEach
  void setUp() {
    repository = mock(IStatisticsEventRepository.class);
    service = new StatisticsService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void aggregatesActivityAndWeatherEvents() {
    stubEvents(
        event(StatisticsEventType.ACTIVITY_CREATED, null, null),
        event(
            StatisticsEventType.ACTIVITY_RESCHEDULED,
            ActivityTransitionReason.VOTATION_RESOLVED,
            null),
        event(StatisticsEventType.ACTIVITY_CANCELLED, ActivityTransitionReason.BAD_WEATHER, null),
        event(
            StatisticsEventType.ACTIVITY_CANCELLED,
            ActivityTransitionReason.QUORUM_NOT_REACHED,
            null),
        event(StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED, null, 100L),
        event(StatisticsEventType.WEATHER_PROVIDER_FAILED, null, 300L));

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
    stubEvents();

    StatisticsResponse response = service.getStatistics(null, null);

    assertThat(response.from()).isEqualTo(NOW.minusSeconds(7 * 24 * 60 * 60));
    assertThat(response.to()).isEqualTo(NOW);
    assertThat(response.activities().created()).isZero();
    assertThat(response.weatherProvider().requests()).isZero();
    assertThat(response.weatherProvider().averageResponseTimeMs()).isZero();
  }

  @Test
  void reportsEachProviderWithItsErrorsRefusalsCacheAndLatestStatus() {
    stubEvents(
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED, null, NOW, 10L),
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_FAILED, "TIMEOUT", NOW, 1500L),
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_FAILED, "TIMEOUT", NOW, 1500L),
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_FAILED, "MALFORMED_RESPONSE", NOW, 5L),
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_REFUSED, "CIRCUIT_OPEN", NOW, null),
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_FALLBACK, null, NOW, null),
        snapshot("OK", "CLOSED", NOW.minusSeconds(120), 3, 1),
        snapshot("DOWN", "OPEN", NOW.minusSeconds(60), 1, 1),
        event(StatisticsEventType.WEATHER_PROVIDER_SUCCEEDED, null, 100L));

    StatisticsResponse response = service.getStatistics(NOW.minusSeconds(600), NOW);

    var legacy =
        response.providers().stream().filter(p -> p.provider().equals("Legacy")).findFirst();
    assertThat(legacy).isPresent();
    assertThat(legacy.get().requests()).isEqualTo(4);
    assertThat(legacy.get().successful()).isEqualTo(1);
    assertThat(legacy.get().errors())
        .containsEntry("TIMEOUT", 2L)
        .containsEntry("MALFORMED_RESPONSE", 1L);
    assertThat(legacy.get().refusals()).containsEntry("CIRCUIT_OPEN", 1L);
    assertThat(legacy.get().fallbacks()).isEqualTo(1);
    assertThat(legacy.get().cacheHits()).isEqualTo(4);
    assertThat(legacy.get().cacheMisses()).isEqualTo(2);
    assertThat(legacy.get().cacheHitRatio()).isEqualTo(4 / 6.0);
    assertThat(legacy.get().status()).isEqualTo("DOWN");
    assertThat(legacy.get().circuitBreakerState()).isEqualTo("OPEN");
    assertThat(legacy.get().timeline()).extracting("status").containsExactly("OK", "DOWN");
    assertThat(response.weatherProvider().requests()).isEqualTo(1);
  }

  @Test
  void summarizesALongTimelineKeepingTheWorstStatusOfEachStretch() {
    StatisticsEvent[] snapshots = new StatisticsEvent[240];
    for (int index = 0; index < snapshots.length; index++) {
      String status = index == 101 ? "DOWN" : "OK";
      snapshots[index] = snapshot(status, "CLOSED", NOW.minusSeconds(240 - index), 0, 0);
    }
    stubEvents(snapshots);

    StatisticsResponse response = service.getStatistics(NOW.minusSeconds(600), NOW);

    var timeline =
        response.providers().stream()
            .filter(p -> p.provider().equals("Legacy"))
            .findFirst()
            .orElseThrow()
            .timeline();
    assertThat(timeline).hasSize(120);
    assertThat(timeline).extracting("status").contains("DOWN");
  }

  @Test
  void aProviderWithoutLookupsHasNoHitRatioAndNoStatus() {
    stubEvents();

    StatisticsResponse response = service.getStatistics(NOW.minusSeconds(60), NOW);

    assertThat(response.providers()).hasSize(2);
    assertThat(response.providers())
        .allSatisfy(
            p -> {
              assertThat(p.cacheHitRatio()).isNull();
              assertThat(p.status()).isNull();
            });
  }

  @Test
  void rejectsInvertedRange() {
    assertThatThrownBy(() -> service.getStatistics(NOW, NOW.minusSeconds(1)))
        .isInstanceOf(InvalidStatisticsRangeException.class);
  }

  @Test
  void passesInclusiveRangeBoundariesToRepository() {
    Instant from = Instant.parse("2026-03-08T05:00:00Z");
    Instant to = Instant.parse("2026-03-09T03:59:59.999Z");
    stubEvents();

    service.getStatistics(from, to);

    verify(repository).findOccurredBetween(from, to);
  }

  @Test
  void addsUpRefusalsAndFallbacksStoredAsCounts() {
    stubEvents(
        counted(StatisticsEventType.WEATHER_PROVIDER_REFUSED, "CIRCUIT_OPEN", 120),
        counted(StatisticsEventType.WEATHER_PROVIDER_REFUSED, "CIRCUIT_OPEN", 30),
        counted(StatisticsEventType.WEATHER_PROVIDER_REFUSED, "OVERLOADED", 4),
        counted(StatisticsEventType.WEATHER_PROVIDER_FALLBACK, null, 154),
        // Recorded one by one before refusals and fallbacks were counted in memory.
        providerEvent(StatisticsEventType.WEATHER_PROVIDER_FALLBACK, null, NOW, null));

    StatisticsResponse response = service.getStatistics(NOW.minusSeconds(600), NOW);

    var legacy =
        response.providers().stream()
            .filter(p -> p.provider().equals("Legacy"))
            .findFirst()
            .orElseThrow();
    assertThat(legacy.refusals())
        .containsEntry("CIRCUIT_OPEN", 150L)
        .containsEntry("OVERLOADED", 4L);
    assertThat(legacy.fallbacks()).isEqualTo(155);
    assertThat(legacy.requests()).isZero();
  }

  private void stubEvents(StatisticsEvent... events) {
    when(repository.findOccurredBetween(any(), any())).thenReturn(Arrays.asList(events));
  }

  private StatisticsEvent counted(StatisticsEventType type, String outcome, long occurrences) {
    return new StatisticsEvent(
        null, type, NOW, null, null, null, "Legacy", outcome, null, null, null, occurrences);
  }

  private StatisticsEvent providerEvent(
      StatisticsEventType type, String outcome, Instant at, Long durationMs) {
    return new StatisticsEvent(
        null, type, at, null, null, durationMs, "Legacy", outcome, null, null, null, null);
  }

  private StatisticsEvent snapshot(
      String status, String breaker, Instant at, long hits, long misses) {
    return new StatisticsEvent(
        null,
        StatisticsEventType.WEATHER_PROVIDER_SNAPSHOT,
        at,
        null,
        null,
        null,
        "Legacy",
        status,
        hits,
        misses,
        breaker,
        null);
  }

  private StatisticsEvent event(
      StatisticsEventType type, ActivityTransitionReason reason, Long durationMilliseconds) {
    return new StatisticsEvent(
        null, type, NOW, "activity-1", reason, durationMilliseconds, "Open-Meteo");
  }
}
