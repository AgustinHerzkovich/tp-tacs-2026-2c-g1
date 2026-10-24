package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.entity.statistics.ProviderStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;

class WeatherProviderMonitorTest {

  @Test
  void aProviderIsDownWhenItsCircuitIsOpenOrItsProcessDoesNotAnswer() {
    assertThat(WeatherProviderMonitor.statusOf(State.OPEN, -1, true))
        .isEqualTo(ProviderStatus.DOWN);
    assertThat(WeatherProviderMonitor.statusOf(State.CLOSED, -1, false))
        .isEqualTo(ProviderStatus.DOWN);
  }

  @Test
  void aProviderIsDegradedWhileRecoveringOrFailingOften() {
    assertThat(WeatherProviderMonitor.statusOf(State.HALF_OPEN, -1, true))
        .isEqualTo(ProviderStatus.DEGRADED);
    assertThat(WeatherProviderMonitor.statusOf(State.CLOSED, 30f, true))
        .isEqualTo(ProviderStatus.DEGRADED);
  }

  @Test
  void aProviderIsOkWithAClosedCircuitAndFewFailures() {
    assertThat(WeatherProviderMonitor.statusOf(State.CLOSED, 10f, true))
        .isEqualTo(ProviderStatus.OK);
    assertThat(WeatherProviderMonitor.statusOf(State.CLOSED, -1, true))
        .isEqualTo(ProviderStatus.OK);
  }

  @Test
  void recordsCacheCountersAsTheDifferenceSinceThePreviousSnapshot() {
    LegacyWeatherProvider legacy = mock(LegacyWeatherProvider.class);
    when(legacy.circuitBreakerState()).thenReturn(State.CLOSED);
    when(legacy.failureRate()).thenReturn(-1f);
    when(legacy.isAlive()).thenReturn(true);
    when(legacy.cacheStats()).thenReturn(stats(5, 2)).thenReturn(stats(8, 3));
    StatisticsEventRecorder recorder = mock(StatisticsEventRecorder.class);
    WeatherProviderMonitor monitor =
        new WeatherProviderMonitor(
            legacy, CircuitBreakerRegistry.ofDefaults(), new NoOpCacheManager(), recorder);

    monitor.snapshot();
    monitor.snapshot();

    verify(recorder).recordProviderSnapshot("Legacy", "OK", "CLOSED", 5, 2);
    verify(recorder).recordProviderSnapshot("Legacy", "OK", "CLOSED", 3, 1);
    verify(recorder, org.mockito.Mockito.times(2))
        .recordProviderSnapshot(eq("Open-Meteo"), anyString(), anyString(), anyLong(), anyLong());
  }

  @Test
  void storesTheCountedRefusalsAndFallbacksWithEachSnapshot() {
    LegacyWeatherProvider legacy = mock(LegacyWeatherProvider.class);
    when(legacy.circuitBreakerState()).thenReturn(State.CLOSED);
    when(legacy.failureRate()).thenReturn(-1f);
    when(legacy.isAlive()).thenReturn(true);
    when(legacy.cacheStats()).thenReturn(stats(0, 0));
    StatisticsEventRecorder recorder = mock(StatisticsEventRecorder.class);

    new WeatherProviderMonitor(
            legacy, CircuitBreakerRegistry.ofDefaults(), new NoOpCacheManager(), recorder)
        .snapshot();

    verify(recorder).flushProviderCounters();
  }

  @Test
  void aFailingLegacySnapshotDoesNotSkipTheRestOfTheRound() {
    LegacyWeatherProvider legacy = mock(LegacyWeatherProvider.class);
    when(legacy.circuitBreakerState()).thenThrow(new IllegalStateException("broken"));
    StatisticsEventRecorder recorder = mock(StatisticsEventRecorder.class);

    new WeatherProviderMonitor(
            legacy, CircuitBreakerRegistry.ofDefaults(), new NoOpCacheManager(), recorder)
        .snapshot();

    verify(recorder)
        .recordProviderSnapshot(eq("Open-Meteo"), anyString(), anyString(), anyLong(), anyLong());
    verify(recorder).flushProviderCounters();
  }

  private static CacheStats stats(long hits, long misses) {
    return CacheStats.of(hits, misses, 0, 0, 0, 0, 0);
  }
}
