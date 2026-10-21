package com.solnotfound.adapters.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.entity.weather.WeatherForecast;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LegacyWeatherClientTest {

  private static final Instant HOUR = Instant.parse("2026-10-22T18:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private LegacyServiceStub stub;

  @BeforeEach
  void setUp() throws IOException {
    stub = new LegacyServiceStub();
  }

  @AfterEach
  void tearDown() {
    stub.close();
  }

  @Test
  void returnsAValidForecastAtTheRequestedLocalTime() {
    stub.then(LegacyServiceStub.valid());

    WeatherForecast forecast = client(stub.baseUrl()).forecast(-34.6037, -58.3816, HOUR, ZONE);

    assertThat(forecast.getDateTime()).isEqualTo(LocalDateTime.of(2026, 10, 22, 15, 0));
    assertThat(forecast.getTemperature()).isEqualTo(22.5f);
    assertThat(forecast.getChanceOfRain()).isEqualTo(9f);
    assertThat(forecast.getWindSpeed()).isEqualTo(35.2f);
  }

  @Test
  void rejectsAnHtmlErrorPageSentWithStatus200() {
    stub.then(
        LegacyServiceStub.body(
            200, "<html><head><title>502 Bad Gateway</title></head><body></body></html>"));

    assertFailure(LegacyFailure.MALFORMED_RESPONSE);
  }

  @Test
  void rejectsJsonWithATrailingComma() {
    stub.then(LegacyServiceStub.trailingComma());

    assertFailure(LegacyFailure.MALFORMED_RESPONSE);
  }

  @Test
  void rejectsAResponseCutInHalf() {
    stub.then(LegacyServiceStub.truncated());

    assertFailure(LegacyFailure.MALFORMED_RESPONSE);
  }

  @Test
  void rejectsAnAnswerToAnotherRequest() {
    stub.then(LegacyServiceStub.otherId());

    assertFailure(LegacyFailure.MISMATCHED_ID);
  }

  @Test
  void treatsAnInvalidRequestAsNotRetryable() {
    stub.then(LegacyServiceStub.error("INVALID_PARAMS"));

    assertFailure(LegacyFailure.REJECTED_REQUEST);
    assertThat(LegacyFailure.REJECTED_REQUEST.isRetryable()).isFalse();
  }

  @Test
  void treatsAnUnknownProviderErrorAsRetryable() {
    stub.then(LegacyServiceStub.error("INTERNAL"));

    assertFailure(LegacyFailure.PROVIDER_ERROR);
  }

  @Test
  void rejectsAnOkResultWithImpossibleValues() {
    stub.then(
        LegacyServiceStub.result(
            LegacyServiceStub.VALID_RESULT
                .formatted(HOUR)
                .replace("\"humidity_pct\":54", "\"humidity_pct\":140")));

    assertFailure(LegacyFailure.INVALID_RESULT);
  }

  @Test
  void rejectsAnOkResultWithoutAField() {
    stub.then(
        LegacyServiceStub.result(
            LegacyServiceStub.VALID_RESULT.formatted(HOUR).replace("\"wind_kph\":35.2,", "")));

    assertFailure(LegacyFailure.INVALID_RESULT);
  }

  @Test
  void rejectsAForecastForAnotherHour() {
    stub.then(
        LegacyServiceStub.result(LegacyServiceStub.VALID_RESULT.formatted("2026-10-22T19:00:00Z")));

    assertFailure(LegacyFailure.INVALID_RESULT);
  }

  @Test
  void rejectsANon200Status() {
    stub.then(LegacyServiceStub.body(503, "unresponsive"));

    assertFailure(LegacyFailure.HTTP_ERROR);
  }

  @Test
  void stopsWaitingForAServiceThatHangs() {
    stub.then(LegacyServiceStub.hang(Duration.ofSeconds(3)));
    long startedAt = System.nanoTime();

    assertFailure(LegacyFailure.TIMEOUT);

    assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(2));
  }

  @Test
  void reportsAServiceThatIsDownAsUnreachable() throws IOException {
    URI closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = URI.create("http://127.0.0.1:" + socket.getLocalPort());
    }

    assertThatThrownBy(() -> client(closedPort).forecast(-34.6, -58.38, HOUR, ZONE))
        .isInstanceOfSatisfying(
            LegacyProviderException.class,
            exception -> assertThat(exception.getFailure()).isEqualTo(LegacyFailure.UNREACHABLE));
  }

  private void assertFailure(LegacyFailure expected) {
    assertThatThrownBy(() -> client(stub.baseUrl()).forecast(-34.6, -58.38, HOUR, ZONE))
        .isInstanceOfSatisfying(
            LegacyProviderException.class,
            exception -> assertThat(exception.getFailure()).isEqualTo(expected));
  }

  static LegacyWeatherClient client(URI baseUrl) {
    return new LegacyWeatherClient(properties(baseUrl), JsonMapper.builder().build());
  }

  static LegacyWeatherProperties properties(URI baseUrl) {
    return new LegacyWeatherProperties(
        baseUrl,
        "test-token",
        null,
        Duration.ofMillis(500),
        Duration.ofMillis(500),
        Duration.ofHours(1),
        100,
        30,
        null);
  }
}
