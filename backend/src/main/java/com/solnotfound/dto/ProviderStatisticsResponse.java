package com.solnotfound.dto;

import java.util.List;
import java.util.Map;

/**
 * Usage and health of one weather provider within the requested range.
 *
 * @param status latest observed status, or {@code null} when it was not observed in the range
 * @param requests calls that reached the provider
 * @param cacheHitRatio hits over hits plus misses, or {@code null} without lookups
 * @param fallbacks lookups for users of this provider answered by the main provider instead
 * @param refusals calls refused before reaching the provider, by reason
 */
public record ProviderStatisticsResponse(
    String provider,
    String status,
    String circuitBreakerState,
    long requests,
    long successful,
    long failed,
    double averageResponseTimeMs,
    long cacheHits,
    long cacheMisses,
    Double cacheHitRatio,
    long fallbacks,
    Map<String, Long> errors,
    Map<String, Long> refusals,
    List<ProviderStatusPoint> timeline) {

  public ProviderStatisticsResponse {
    errors = Map.copyOf(errors);
    refusals = Map.copyOf(refusals);
    timeline = List.copyOf(timeline);
  }

  @Override
  public Map<String, Long> errors() {
    return Map.copyOf(errors);
  }

  @Override
  public Map<String, Long> refusals() {
    return Map.copyOf(refusals);
  }

  @Override
  public List<ProviderStatusPoint> timeline() {
    return List.copyOf(timeline);
  }
}
