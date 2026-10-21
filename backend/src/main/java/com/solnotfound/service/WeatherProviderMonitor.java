package com.solnotfound.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.entity.statistics.ProviderStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Takes a periodic snapshot of each weather provider so administrators can see how it behaved over
 * time: its status, its circuit breaker state and its cache hits and misses since the previous
 * snapshot.
 *
 * <p>A provider is {@link ProviderStatus#DOWN} when its circuit breaker is open or, for the legacy
 * one, when its liveness probe does not answer; {@link ProviderStatus#DEGRADED} when the breaker is
 * half open or at least {@value #DEGRADED_FAILURE_RATE}% of the recent lookups failed; and {@link
 * ProviderStatus#OK} otherwise. Counters live in this instance, so each instance reports its own.
 */
@Slf4j
@Component
public class WeatherProviderMonitor {

  static final float DEGRADED_FAILURE_RATE = 25f;
  private static final String OPEN_METEO_BREAKER = "openMeteo";
  private static final List<String> OPEN_METEO_CACHES =
      List.of("weather-current", "weather-forecast");

  private final LegacyWeatherProvider legacyProvider;
  private final CircuitBreakerRegistry circuitBreakers;
  private final CacheManager cacheManager;
  private final StatisticsEventRecorder statisticsRecorder;

  private CacheStats lastLegacyStats = CacheStats.empty();
  private CacheStats lastOpenMeteoStats = CacheStats.empty();

  public WeatherProviderMonitor(
      LegacyWeatherProvider legacyProvider,
      CircuitBreakerRegistry circuitBreakers,
      CacheManager cacheManager,
      StatisticsEventRecorder statisticsRecorder) {
    this.legacyProvider = legacyProvider;
    this.circuitBreakers = circuitBreakers;
    this.cacheManager = cacheManager;
    this.statisticsRecorder = statisticsRecorder;
  }

  /**
   * Records one snapshot per provider. Failures are logged and never stop the schedule. On Cloud
   * Run the timer is disabled and Cloud Scheduler calls it through {@code
   * /internal/scheduled/weather-providers}, because an idle instance gets no CPU.
   */
  @Scheduled(cron = "${weather.monitor.cron:0 * * * * *}")
  public synchronized void snapshot() {
    try {
      snapshotLegacy();
      snapshotOpenMeteo();
    } catch (RuntimeException exception) {
      log.warn("Could not record weather provider snapshots", exception);
    }
  }

  private void snapshotLegacy() {
    CircuitBreaker.State state = legacyProvider.circuitBreakerState();
    ProviderStatus status = statusOf(state, legacyProvider.failureRate(), legacyProvider.isAlive());
    CacheStats current = legacyProvider.cacheStats();
    CacheStats delta = current.minus(lastLegacyStats);
    lastLegacyStats = current;
    statisticsRecorder.recordProviderSnapshot(
        LegacyWeatherProvider.PROVIDER,
        status.name(),
        state.name(),
        delta.hitCount(),
        delta.missCount());
  }

  private void snapshotOpenMeteo() {
    CircuitBreaker breaker = circuitBreakers.circuitBreaker(OPEN_METEO_BREAKER);
    ProviderStatus status =
        statusOf(breaker.getState(), breaker.getMetrics().getFailureRate(), true);
    CacheStats current =
        OPEN_METEO_CACHES.stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .filter(CaffeineCache.class::isInstance)
            .map(cache -> ((CaffeineCache) cache).getNativeCache().stats())
            .reduce(CacheStats.empty(), CacheStats::plus);
    CacheStats delta = current.minus(lastOpenMeteoStats);
    lastOpenMeteoStats = current;
    statisticsRecorder.recordProviderSnapshot(
        StatisticsEventRecorder.OPEN_METEO,
        status.name(),
        breaker.getState().name(),
        delta.hitCount(),
        delta.missCount());
  }

  static ProviderStatus statusOf(CircuitBreaker.State state, float failureRate, boolean alive) {
    if (!alive || state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN) {
      return ProviderStatus.DOWN;
    }
    if (state == CircuitBreaker.State.HALF_OPEN || failureRate >= DEGRADED_FAILURE_RATE) {
      return ProviderStatus.DEGRADED;
    }
    return ProviderStatus.OK;
  }
}
