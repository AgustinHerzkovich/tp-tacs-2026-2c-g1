package com.solnotfound.adapters.legacy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-process stand-in for the legacy weather service. Each request is answered with the next
 * scripted behavior, reproducing the failures seen in the real binary: hangs, broken JSON, HTML
 * error pages with status 200, answers for another request, and so on. Once the script runs out, it
 * answers with a valid forecast.
 */
final class LegacyServiceStub implements AutoCloseable {

  static final String VALID_RESULT =
      """
      {"location":{"lat":-34.6,"lon":-58.38},"timestamp":"%s","condition":"clear",\
      "temperature_c":22.5,"feels_like_c":22.5,"humidity_pct":54,\
      "precipitation_probability_pct":9,"precipitation_mm":0,"wind_kph":35.2,\
      "wind_direction":"SE"}""";

  private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern TIMESTAMP = Pattern.compile("\"timestamp\"\\s*:\\s*\"([^\"]+)\"");

  private final HttpServer server;
  private final Deque<Behavior> script = new ArrayDeque<>();
  private final AtomicInteger requests = new AtomicInteger();

  @FunctionalInterface
  interface Behavior {
    void answer(HttpExchange exchange, String id, String timestamp) throws IOException;
  }

  LegacyServiceStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    server.createContext("/rpc", this::handle);
    server.start();
  }

  URI baseUrl() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  int requests() {
    return requests.get();
  }

  synchronized LegacyServiceStub then(Behavior behavior) {
    script.addLast(behavior);
    return this;
  }

  static Behavior valid() {
    return (exchange, id, timestamp) ->
        send(exchange, 200, envelope(id, "OK", VALID_RESULT.formatted(timestamp)));
  }

  static Behavior body(int status, String body) {
    return (exchange, id, timestamp) -> send(exchange, status, body);
  }

  static Behavior trailingComma() {
    return (exchange, id, timestamp) ->
        send(exchange, 200, envelope(id, "OK", VALID_RESULT.formatted(timestamp)) + ",}");
  }

  static Behavior truncated() {
    return (exchange, id, timestamp) -> {
      String full = envelope(id, "OK", VALID_RESULT.formatted(timestamp));
      send(exchange, 200, full.substring(0, full.length() / 2));
    };
  }

  static Behavior otherId() {
    return (exchange, id, timestamp) ->
        send(exchange, 200, envelope("someone-else", "OK", VALID_RESULT.formatted(timestamp)));
  }

  static Behavior error(String code) {
    return (exchange, id, timestamp) ->
        send(
            exchange,
            200,
            "{\"id\":\"%s\",\"status\":\"ERROR\",\"error\":{\"code\":\"%s\",\"message\":\"x\"}}"
                .formatted(id, code));
  }

  static Behavior result(String result) {
    return (exchange, id, timestamp) -> send(exchange, 200, envelope(id, "OK", result));
  }

  static Behavior hang(Duration duration) {
    return (exchange, id, timestamp) -> {
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
      send(exchange, 200, envelope(id, "OK", VALID_RESULT.formatted(timestamp)));
    };
  }

  private void handle(HttpExchange exchange) throws IOException {
    requests.incrementAndGet();
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Behavior behavior;
    synchronized (this) {
      behavior = script.isEmpty() ? valid() : script.removeFirst();
    }
    behavior.answer(exchange, group(ID, body), group(TIMESTAMP, body));
  }

  private static String group(Pattern pattern, String body) {
    Matcher matcher = pattern.matcher(body);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static String envelope(String id, String status, String result) {
    return "{\"id\":\"%s\",\"status\":\"%s\",\"result\":%s}".formatted(id, status, result);
  }

  private static void send(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
