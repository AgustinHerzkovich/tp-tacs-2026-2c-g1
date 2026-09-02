package com.solnotfound.adapters.openmeteo;

import com.solnotfound.exception.WeatherUnavailableException;
import com.solnotfound.service.StatisticsEventRecorder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenMeteoClient {

  private static final String WEATHER_FIELDS =
      "temperature_2m,precipitation_probability,wind_speed_10m";

  private final RestClient restClient;
  private final OpenMeteoProperties properties;
  private final StatisticsEventRecorder statisticsRecorder;

  @Autowired
  public OpenMeteoClient(
      RestClient.Builder builder,
      OpenMeteoProperties properties,
      StatisticsEventRecorder statisticsRecorder) {
    this.restClient = builder.build();
    this.properties = properties;
    this.statisticsRecorder = statisticsRecorder;
  }

  @Cacheable(cacheNames = "weather-current", key = "#latitude + ',' + #longitude", sync = true)
  @CircuitBreaker(name = "openMeteo")
  @Retry(name = "openMeteo")
  public OpenMeteoDtos.ForecastResponse getCurrent(Double latitude, Double longitude) {
    return executeRequest(
        () ->
            restClient
                .get()
                .uri(
                    properties.forecastUrl().toString(),
                    builder ->
                        builder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", WEATHER_FIELDS)
                            .queryParam("timezone", "auto")
                            .build())
                .retrieve()
                .body(OpenMeteoDtos.ForecastResponse.class));
  }

  /**
   * Retrieves and caches one complete hourly range. Failed calls are retried briefly and contribute
   * to the provider circuit breaker; failed values are never cached.
   *
   * @param latitude WGS84 latitude
   * @param longitude WGS84 longitude
   * @param startDate first local date requested
   * @param endDate last local date requested
   * @return raw Open-Meteo response
   */
  @Cacheable(
      cacheNames = "weather-forecast",
      key = "#latitude + ',' + #longitude + '|' + #startDate + '|' + #endDate",
      sync = true)
  @CircuitBreaker(name = "openMeteo")
  @Retry(name = "openMeteo")
  public OpenMeteoDtos.ForecastResponse getForecast(
      Double latitude, Double longitude, LocalDate startDate, LocalDate endDate) {
    return executeRequest(
        () ->
            restClient
                .get()
                .uri(
                    properties.forecastUrl().toString(),
                    builder ->
                        builder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("hourly", WEATHER_FIELDS)
                            .queryParam("timezone", "auto")
                            .queryParam("start_date", startDate)
                            .queryParam("end_date", endDate)
                            .build())
                .retrieve()
                .body(OpenMeteoDtos.ForecastResponse.class));
  }

  @Cacheable(cacheNames = "weather-geocoding", key = "#city.toLowerCase()", sync = true)
  @CircuitBreaker(name = "openMeteo")
  @Retry(name = "openMeteo")
  public OpenMeteoDtos.GeocodingResponse geocode(String city) {
    return executeRequest(
        () ->
            restClient
                .get()
                .uri(
                    properties.geocodingUrl().toString(),
                    builder ->
                        builder
                            .queryParam("name", city)
                            .queryParam("count", 1)
                            .queryParam("language", "es")
                            .queryParam("format", "json")
                            .build())
                .retrieve()
                .body(OpenMeteoDtos.GeocodingResponse.class));
  }

  private <T> T executeRequest(Supplier<T> request) {
    long startedAt = System.nanoTime();
    try {
      T response = request.get();
      statisticsRecorder.recordWeatherRequest(true, elapsedMilliseconds(startedAt));
      return response;
    } catch (RestClientException exception) {
      statisticsRecorder.recordWeatherRequest(false, elapsedMilliseconds(startedAt));
      throw unavailable(exception);
    }
  }

  private long elapsedMilliseconds(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }

  private WeatherUnavailableException unavailable(RestClientException exception) {
    return new WeatherUnavailableException("Open-Meteo is temporarily unavailable", exception);
  }
}
