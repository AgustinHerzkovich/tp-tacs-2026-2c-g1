package com.solnotfound.adapters.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

final class OpenMeteoDtos {

  private OpenMeteoDtos() {}

  record ForecastResponse(Current current, Hourly hourly) {}

  record Current(
      LocalDateTime time,
      @JsonProperty("temperature_2m") Float temperature,
      @JsonProperty("precipitation_probability") Float rainProbability,
      @JsonProperty("wind_speed_10m") Float windSpeed) {}

  record Hourly(
      List<LocalDateTime> time,
      @JsonProperty("temperature_2m") List<Float> temperatures,
      @JsonProperty("precipitation_probability") List<Float> rainProbabilities,
      @JsonProperty("wind_speed_10m") List<Float> windSpeeds) {}

  record GeocodingResponse(List<GeocodingResult> results) {}

  record GeocodingResult(Double latitude, Double longitude) {}
}
