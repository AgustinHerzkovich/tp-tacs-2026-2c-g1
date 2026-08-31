package com.solnotfound.adapters.openmeteo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.entity.activity.City;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.WeatherUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenMeteoWeatherAdapterTest {

  private HttpServer server;
  private URI endpoint;
  private final AtomicInteger requests = new AtomicInteger();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    endpoint = URI.create("http://localhost:" + server.getAddress().getPort());
    server.start();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void mapsCurrentWeatherUsingCoordinates() {
    respond(
        "/forecast",
        """
        {"current":{"time":"2026-08-30T18:00","temperature_2m":21.5,
        "precipitation_probability":35,"wind_speed_10m":14.2}}
        """);

    WeatherForecast result = adapter().getWeather(location());

    assertThat(result.getDateTime()).isEqualTo(LocalDateTime.of(2026, 8, 30, 18, 0));
    assertThat(result.getTemperature()).isEqualTo(21.5f);
    assertThat(result.getChanceOfRain()).isEqualTo(35.0f);
    assertThat(result.getWindSpeed()).isEqualTo(14.2f);
  }

  @Test
  void fetchesRangeOnceAndRoundsHalfHourTowardFuture() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    respond("/forecast", hourlyResponse(tomorrow));
    OpenMeteoWeatherAdapter adapter = adapter();
    LocalDateTime first = tomorrow.atTime(10, 29);
    LocalDateTime second = tomorrow.atTime(10, 30);

    List<WeatherForecast> results = adapter.getForecastRange(location(), List.of(first, second));

    assertThat(requests).hasValue(1);
    assertThat(results).extracting(WeatherForecast::getDateTime).containsExactly(first, second);
    assertThat(results).extracting(WeatherForecast::getTemperature).containsExactly(20.0f, 22.0f);
  }

  @Test
  void geocodesCityUsingFirstResult() {
    respond(
        "/geocoding",
        """
        {"results":[{"latitude":-34.6037,"longitude":-58.3816}]}
        """);
    respond("/forecast", hourlyResponse(LocalDate.now().plusDays(1)));
    Location cityOnly = new Location(new City("ba", "Buenos Aires"), null, null);

    WeatherForecast result =
        adapter().getFutureClimate(cityOnly, LocalDate.now().plusDays(1).atTime(10, 0));

    assertThat(result.getTemperature()).isEqualTo(20.0f);
    assertThat(requests).hasValue(2);
  }

  @Test
  void rejectsDateOutsideProviderHorizonWithoutCallingProvider() {
    LocalDateTime outsideHorizon = LocalDate.now().plusDays(16).atStartOfDay();

    assertThatThrownBy(() -> adapter().getFutureClimate(location(), outsideHorizon))
        .isInstanceOf(WeatherUnavailableException.class)
        .hasMessageContaining("outside the 16-day provider horizon");
    assertThat(requests).hasValue(0);
  }

  @Test
  void rejectsIncompleteProviderResponse() {
    respond("/forecast", "{\"hourly\":{\"time\":[]}}");

    assertThatThrownBy(
            () -> adapter().getFutureClimate(location(), LocalDate.now().plusDays(1).atTime(10, 0)))
        .isInstanceOf(WeatherUnavailableException.class)
        .hasMessageContaining("incomplete");
  }

  private OpenMeteoWeatherAdapter adapter() {
    OpenMeteoProperties properties =
        new OpenMeteoProperties(endpoint.resolve("/forecast"), endpoint.resolve("/geocoding"));
    return new OpenMeteoWeatherAdapter(new OpenMeteoClient(RestClient.builder(), properties));
  }

  private Location location() {
    return new Location(null, -34.6037, -58.3816);
  }

  private String hourlyResponse(LocalDate date) {
    return """
        {"hourly":{"time":["%sT10:00","%sT11:00"],
        "temperature_2m":[20.0,22.0],"precipitation_probability":[10,20],
        "wind_speed_10m":[5.0,7.0]}}
        """
        .formatted(date, date);
  }

  private void respond(String path, String body) {
    server.createContext(
        path,
        exchange -> {
          requests.incrementAndGet();
          send(exchange, body);
        });
  }

  private void send(HttpExchange exchange, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
