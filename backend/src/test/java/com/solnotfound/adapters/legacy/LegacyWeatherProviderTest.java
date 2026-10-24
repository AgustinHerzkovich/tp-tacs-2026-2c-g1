package com.solnotfound.adapters.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.service.StatisticsEventRecorder;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the resilience of the legacy provider against an in-process stub, with Resilience4j
 * configured like {@code application.properties} but with shorter waits.
 */
class LegacyWeatherProviderTest {

  private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final Location PLACE = new Location(null, -34.6037, -58.3816);
  private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 22, 15, 0);

  private LegacyServiceStub stub;
  private StatisticsEventRecorder statistics;
  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() throws IOException {
    stub = new LegacyServiceStub();
    statistics = mock(StatisticsEventRecorder.class);
    circuitBreaker =
        CircuitBreaker.of(
            "legacyWeather",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .recordException(new LegacyProviderFaultPredicate())
                .ignoreException(new LegacyRefusalPredicate())
                .build());
  }

  @AfterEach
  void tearDown() {
    stub.close();
  }

  @Test
  void retriesTransientFailuresUntilAValidAnswerArrives() {
    stub.then(LegacyServiceStub.trailingComma()).then(LegacyServiceStub.otherId());

    WeatherForecast forecast = provider(10).forecast(PLACE, WHEN, ZONE);

    assertThat(forecast.getTemperature()).isEqualTo(22.5f);
    assertThat(stub.requests()).isEqualTo(3);
    verify(statistics)
        .recordProviderCall(eq("Legacy"), eq(false), anyLong(), eq("MALFORMED_RESPONSE"));
    verify(statistics).recordProviderCall(eq("Legacy"), eq(false), anyLong(), eq("MISMATCHED_ID"));
  }

  @Test
  void doesNotRetryARequestTheServiceRejects() {
    stub.then(LegacyServiceStub.error("INVALID_PARAMS"));

    assertThatThrownBy(() -> provider(10).forecast(PLACE, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isEqualTo(1);
  }

  @Test
  void doesNotRetryAServiceThatIsDown() throws IOException {
    java.net.URI closedPort;
    try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
      closedPort = java.net.URI.create("http://127.0.0.1:" + socket.getLocalPort());
    }
    LegacyWeatherProvider provider = providerFor(LegacyWeatherClientTest.client(closedPort), 10);

    assertThatThrownBy(() -> provider.forecast(PLACE, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    verify(statistics).recordProviderCall(eq("Legacy"), eq(false), anyLong(), eq("UNREACHABLE"));
  }

  @Test
  void givesUpAfterThreeAttempts() {
    for (int index = 0; index < 3; index++) {
      stub.then(LegacyServiceStub.truncated());
    }

    assertThatThrownBy(() -> provider(10).forecast(PLACE, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isEqualTo(3);
  }

  @Test
  void servesARepeatedLookupFromTheCache() {
    LegacyWeatherProvider provider = provider(10);

    provider.forecast(PLACE, WHEN, ZONE);
    WeatherForecast again = provider.forecast(PLACE, WHEN.plusMinutes(20), ZONE);

    assertThat(stub.requests()).isEqualTo(1);
    assertThat(again.getDateTime()).isEqualTo(WHEN.plusMinutes(20));
    assertThat(provider.cacheStats().hitCount()).isEqualTo(1);
  }

  @Test
  void aCachedForecastDoesNotSpendTheCallersQuota() {
    LegacyWeatherProvider provider = provider(10);
    provider.forecast(PLACE, WHEN, ZONE);

    WeatherForecast cached = provider.forecast(PLACE, WHEN, ZONE, () -> false);

    assertThat(cached.getTemperature()).isEqualTo(22.5f);
    assertThat(stub.requests()).isEqualTo(1);
  }

  @Test
  void aRefusedQuotaNeverReachesTheService() {
    assertThatThrownBy(() -> provider(10).forecast(PLACE, WHEN, ZONE, () -> false))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isZero();
  }

  @Test
  void neverCachesAFailure() {
    for (int index = 0; index < 3; index++) {
      stub.then(LegacyServiceStub.body(200, "<html>502 Bad Gateway</html>"));
    }
    LegacyWeatherProvider provider = provider(10);

    assertThatThrownBy(() -> provider.forecast(PLACE, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    WeatherForecast recovered = provider.forecast(PLACE, WHEN, ZONE);

    assertThat(recovered.getTemperature()).isEqualTo(22.5f);
  }

  @Test
  void opensTheCircuitWhenTheServiceKeepsFailingAndStopsCallingIt() {
    for (int index = 0; index < 12; index++) {
      stub.then(LegacyServiceStub.body(503, "unresponsive"));
    }
    LegacyWeatherProvider provider = provider(10);

    for (int hour = 0; hour < 4; hour++) {
      LocalDateTime when = WHEN.plusHours(hour);
      assertThatThrownBy(() -> provider.forecast(PLACE, when, ZONE))
          .isInstanceOf(LegacyProviderUnavailableException.class);
    }
    int requestsWhenOpened = stub.requests();

    assertThat(provider.circuitBreakerState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> provider.forecast(PLACE, WHEN.plusHours(10), ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isEqualTo(requestsWhenOpened);
    verify(statistics).recordProviderRefusal("Legacy", "CIRCUIT_OPEN");
  }

  @Test
  void turnsAwayCallsBeyondTheConcurrencyLimitInsteadOfQueuing() throws Exception {
    stub.then(LegacyServiceStub.hang(Duration.ofMillis(400)));
    LegacyWeatherProvider provider = provider(1);
    ExecutorService pool = Executors.newSingleThreadExecutor();
    CountDownLatch started = new CountDownLatch(1);
    try {
      pool.submit(
          () -> {
            started.countDown();
            return provider.forecast(PLACE, WHEN, ZONE);
          });
      started.await(1, TimeUnit.SECONDS);
      Thread.sleep(100);

      assertThatThrownBy(() -> provider.forecast(PLACE, WHEN.plusHours(1), ZONE))
          .isInstanceOf(LegacyProviderUnavailableException.class);
      verify(statistics, atLeastOnce()).recordProviderRefusal("Legacy", "OVERLOADED");
      // The turned-away lookup never reached the service: the breaker does not count it at all.
      assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void anUnexpectedFailureStillEndsInTheFallback() {
    LegacyWeatherClient client =
        new LegacyWeatherClient(
            java.net.http.HttpClient.newHttpClient(),
            LegacyWeatherClientTest.properties(stub.baseUrl()),
            tools.jackson.databind.json.JsonMapper.builder().build(),
            () -> {
              throw new IllegalStateException("identity token library failed");
            });

    assertThatThrownBy(() -> providerFor(client, 10).forecast(PLACE, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isZero();
  }

  @Test
  void needsCoordinatesBecauseTheServiceTakesNoPlaceNames() {
    Location cityOnly = new Location(null, null, null);

    assertThatThrownBy(() -> provider(10).forecast(cityOnly, WHEN, ZONE))
        .isInstanceOf(LegacyProviderUnavailableException.class);
    assertThat(stub.requests()).isZero();
  }

  private LegacyWeatherProvider provider(int maxConcurrentCalls) {
    return providerFor(LegacyWeatherClientTest.client(stub.baseUrl()), maxConcurrentCalls);
  }

  private LegacyWeatherProvider providerFor(LegacyWeatherClient client, int maxConcurrentCalls) {
    Retry retry =
        Retry.of(
            "legacyWeather",
            RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(10, 2, 0.5))
                .retryOnException(new LegacyRetryPredicate())
                .build());
    RateLimiter rateLimiter =
        RateLimiter.of(
            "legacyWeather",
            RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build());
    Bulkhead bulkhead =
        Bulkhead.of(
            "legacyWeather",
            BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(Duration.ZERO)
                .build());
    return new LegacyWeatherProvider(
        client,
        circuitBreaker,
        retry,
        rateLimiter,
        bulkhead,
        statistics,
        LegacyWeatherClientTest.properties(stub.baseUrl()));
  }
}
