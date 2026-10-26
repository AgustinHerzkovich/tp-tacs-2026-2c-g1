package com.solnotfound.dto;

import java.time.Instant;
import java.util.List;

/**
 * Platform statistics for a time range.
 *
 * @param weatherProvider calls to Open-Meteo, kept for clients of the previous version
 */
public record StatisticsResponse(
    Instant from,
    Instant to,
    ActivityStatisticsResponse activities,
    WeatherProviderStatisticsResponse weatherProvider,
    List<ProviderStatisticsResponse> providers) {

  public StatisticsResponse {
    providers = List.copyOf(providers);
  }

  @Override
  public List<ProviderStatisticsResponse> providers() {
    return List.copyOf(providers);
  }
}
