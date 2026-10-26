package com.solnotfound.adapters.legacy;

import com.solnotfound.entity.weather.WeatherForecast;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends one forecast request to the legacy service and turns the answer into a {@link
 * WeatherForecast}, or into a {@link LegacyProviderException} that says how it failed.
 *
 * <p>Nothing the service returns is trusted: it answers 200 with broken JSON, with an HTML error
 * page, with the error inside the body, or for another request id. A response is accepted only when
 * it parses strictly, answers this request, reports {@code OK}, is for the requested hour and
 * place, and every value is physically possible. This class makes a single attempt; retries,
 * circuit breaking and caching are applied by {@link LegacyWeatherProvider}.
 */
public class LegacyWeatherClient {

  private static final String METHOD = "weather.forecast";
  private static final double MAX_COORDINATE_DRIFT = 0.05;

  private final HttpClient httpClient;
  private final URI rpcUri;
  private final String apiToken;
  private final Path apiTokenFile;
  private final Duration readTimeout;
  private final Duration attemptBudget;
  private final ObjectMapper objectMapper;
  private final Supplier<String> serviceIdentity;

  public LegacyWeatherClient(LegacyWeatherProperties properties, ObjectMapper objectMapper) {
    this(
        HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .version(HttpClient.Version.HTTP_1_1)
            .build(),
        properties,
        objectMapper,
        properties.cloudRunAudience() == null || properties.cloudRunAudience().isBlank()
            ? null
            : CloudRunIdTokens.forAudience(properties.cloudRunAudience()));
  }

  LegacyWeatherClient(
      HttpClient httpClient,
      LegacyWeatherProperties properties,
      ObjectMapper objectMapper,
      Supplier<String> serviceIdentity) {
    this.httpClient = httpClient;
    this.rpcUri = properties.baseUrl().resolve("/rpc");
    // Secrets are often stored with a trailing newline, which is not a valid header value.
    this.apiToken = properties.apiToken() == null ? null : properties.apiToken().strip();
    this.apiTokenFile =
        properties.apiTokenFile() == null || properties.apiTokenFile().isBlank()
            ? null
            : Path.of(properties.apiTokenFile());
    this.readTimeout = properties.readTimeout();
    this.attemptBudget = properties.connectTimeout().plus(properties.readTimeout());
    this.objectMapper = objectMapper;
    this.serviceIdentity = serviceIdentity;
  }

  /**
   * Requests the forecast for one place and hour.
   *
   * @param latitude WGS84 latitude
   * @param longitude WGS84 longitude
   * @param hour instant whose hourly forecast is wanted; it is truncated to the hour
   * @param zone zone in which the returned forecast time is expressed
   * @return the validated forecast, timed at {@code hour} in {@code zone}
   * @throws LegacyProviderException when the call fails or the answer cannot be used
   */
  public WeatherForecast forecast(double latitude, double longitude, Instant hour, ZoneId zone) {
    Instant requestedHour = hour.truncatedTo(ChronoUnit.HOURS);
    String requestId = UUID.randomUUID().toString();
    String body = send(requestBody(requestId, latitude, longitude, requestedHour));
    JsonNode result = validEnvelope(body, requestId);
    return forecastFrom(result, latitude, longitude, requestedHour, zone);
  }

  /**
   * Asks the service's liveness probe. It only proves the process answers: the probe says OK even
   * while forecast calls hang, so it is used to tell "down" apart, never to call the service
   * healthy.
   *
   * @return {@code true} when the probe answers 200 within the read timeout; {@code false} on any
   *     failure, including one obtaining the identity token, so the monitor reports it as down
   */
  public boolean isAlive() {
    try {
      HttpRequest request =
          authenticated(HttpRequest.newBuilder(rpcUri.resolve("/healthz")))
              .timeout(readTimeout)
              .GET()
              .build();
      return exchange(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  /**
   * Sends a request within a total time budget. The connect and read timeouts of the HTTP client do
   * not cover resolving the host name, and a stopped container's name took 8 to 9 seconds to fail
   * to resolve; the budget bounds the whole attempt, name resolution included.
   */
  private <T> HttpResponse<T> exchange(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
    CompletableFuture<HttpResponse<T>> pending = httpClient.sendAsync(request, handler);
    try {
      return pending.get(attemptBudget.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException exception) {
      pending.cancel(true);
      throw new LegacyProviderException(
          LegacyFailure.TIMEOUT, "Legacy service did not answer in time", exception);
    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof HttpTimeoutException) {
        throw new LegacyProviderException(
            LegacyFailure.TIMEOUT, "Legacy service did not answer in time", cause);
      }
      throw new LegacyProviderException(
          LegacyFailure.UNREACHABLE, "Legacy service is not reachable", cause);
    } catch (InterruptedException exception) {
      pending.cancel(true);
      Thread.currentThread().interrupt();
      throw new LegacyProviderException(
          LegacyFailure.UNREACHABLE, "Interrupted while calling the legacy service", exception);
    }
  }

  private HttpRequest.Builder authenticated(HttpRequest.Builder builder) {
    if (serviceIdentity != null) {
      builder.header("X-Serverless-Authorization", "Bearer " + serviceIdentity.get());
    }
    return builder;
  }

  private String requestBody(String id, double latitude, double longitude, Instant hour) {
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              "id",
              id,
              "method",
              METHOD,
              "params",
              Map.of(
                  "location",
                  Map.of("lat", latitude, "lon", longitude),
                  "timestamp",
                  hour.toString())));
    } catch (JacksonException exception) {
      throw new IllegalStateException("Could not serialize the legacy request", exception);
    }
  }

  private String send(String requestBody) {
    HttpRequest request =
        authenticated(HttpRequest.newBuilder(rpcUri))
            .timeout(readTimeout)
            .header("Authorization", "Bearer " + currentToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    HttpResponse<String> response = exchange(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new LegacyProviderException(
          LegacyFailure.HTTP_ERROR, "Legacy service answered HTTP " + response.statusCode());
    }
    return response.body();
  }

  /**
   * The configured token, or the one the legacy container wrote to the shared file. The file is
   * read on every call because the container rewrites it after each sign-in.
   */
  private String currentToken() {
    if (apiToken != null && !apiToken.isBlank()) {
      return apiToken;
    }
    if (apiTokenFile == null) {
      throw new LegacyProviderException(
          LegacyFailure.REJECTED_REQUEST, "No API token configured for the legacy service");
    }
    try {
      String token = Files.readString(apiTokenFile, StandardCharsets.UTF_8).trim();
      if (token.isEmpty()) {
        throw new LegacyProviderException(
            LegacyFailure.REJECTED_REQUEST, "The legacy service has not signed in yet");
      }
      return token;
    } catch (IOException exception) {
      throw new LegacyProviderException(
          LegacyFailure.REJECTED_REQUEST, "The legacy service has not signed in yet", exception);
    }
  }

  private JsonNode validEnvelope(String body, String requestId) {
    JsonNode envelope;
    try {
      envelope = objectMapper.readTree(body);
    } catch (JacksonException exception) {
      throw new LegacyProviderException(
          LegacyFailure.MALFORMED_RESPONSE, "Legacy response is not valid JSON", exception);
    }
    if (envelope == null || !envelope.isObject()) {
      throw new LegacyProviderException(
          LegacyFailure.MALFORMED_RESPONSE, "Legacy response is not a JSON object");
    }
    if (!requestId.equals(envelope.path("id").asString(null))) {
      throw new LegacyProviderException(
          LegacyFailure.MISMATCHED_ID, "Legacy response answers another request");
    }
    String status = envelope.path("status").asString("");
    if (!"OK".equals(status)) {
      String code = envelope.path("error").path("code").asString("UNKNOWN");
      boolean requestProblem =
          "INVALID_PARAMS".equals(code)
              || "METHOD_NOT_FOUND".equals(code)
              || "PARSE_ERROR".equals(code)
              || "UNAUTHENTICATED".equals(code);
      throw new LegacyProviderException(
          requestProblem ? LegacyFailure.REJECTED_REQUEST : LegacyFailure.PROVIDER_ERROR,
          "Legacy service reported " + code);
    }
    JsonNode result = envelope.path("result");
    if (!result.isObject()) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy response has no result");
    }
    return result;
  }

  private WeatherForecast forecastFrom(
      JsonNode result, double latitude, double longitude, Instant requestedHour, ZoneId zone) {
    double temperature = number(result, "temperature_c", -60, 60);
    double rainProbability = number(result, "precipitation_probability_pct", 0, 100);
    double windSpeed = number(result, "wind_kph", 0, 400);
    number(result, "humidity_pct", 0, 100);
    number(result, "precipitation_mm", 0, 500);

    Instant answeredHour = instant(result.path("timestamp"));
    if (!requestedHour.equals(answeredHour)) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy forecast is for another hour: " + answeredHour);
    }
    JsonNode location = result.path("location");
    double answeredLatitude = number(location, "lat", -90, 90);
    double answeredLongitude = number(location, "lon", -180, 180);
    if (Math.abs(answeredLatitude - latitude) > MAX_COORDINATE_DRIFT
        || Math.abs(answeredLongitude - longitude) > MAX_COORDINATE_DRIFT) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy forecast is for another place");
    }
    return new WeatherForecast(
        LocalDateTime.ofInstant(requestedHour, zone),
        (float) temperature,
        (float) rainProbability,
        (float) windSpeed);
  }

  private static double number(JsonNode parent, String field, double min, double max) {
    JsonNode value = parent.path(field);
    if (!value.isNumber()) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy result has no numeric " + field);
    }
    double number = value.asDouble();
    if (!Double.isFinite(number) || number < min || number > max) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy result has an impossible " + field);
    }
    return number;
  }

  private static Instant instant(JsonNode value) {
    if (!value.isString()) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy result has no timestamp");
    }
    try {
      return Instant.parse(value.asString());
    } catch (DateTimeParseException exception) {
      throw new LegacyProviderException(
          LegacyFailure.INVALID_RESULT, "Legacy result has an invalid timestamp", exception);
    }
  }
}
