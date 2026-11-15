package com.solnotfound.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.PageResponse;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.exception.InvalidActivityException;
import com.solnotfound.repository.InMemoryActivityRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import com.solnotfound.repository.InMemoryVotationRepository;
import com.solnotfound.service.ActivityService;
import com.solnotfound.service.StatisticsEventRecorder;
import com.solnotfound.service.WeatherGateway;
import com.solnotfound.storage.NoOpImageStorage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class ActivitySearchTelemetryTest {

  private static final PageRequest FIRST_PAGE = PageRequest.of(0, 12);

  private final InMemorySpanExporter spans = InMemorySpanExporter.create();
  private final InMemoryMetricReader metrics = InMemoryMetricReader.create();
  private final OpenTelemetrySdk sdk =
      OpenTelemetrySdk.builder()
          .setTracerProvider(
              SdkTracerProvider.builder()
                  .addSpanProcessor(SimpleSpanProcessor.create(spans))
                  .build())
          .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metrics).build())
          .build();

  @AfterEach
  void closeSdk() {
    sdk.close();
  }

  @Test
  void recordsWhichFiltersWereUsedButNeverTheirValues() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ZERO);
    ActivityFilterDTO filter =
        filter(ActivityType.OUTDOOR, "Palermo", "secret words", LocalDateTime.now(), null);

    telemetry.traceSearch(filter, PageRequest.of(2, 5), () -> page(List.of("a", "b"), 12));

    SpanData span = onlySpan(ActivitySearchTelemetry.SEARCH_SPAN);
    assertThat(span.getAttributes().get(ActivitySearchTelemetry.FILTER_TYPE)).isEqualTo("OUTDOOR");
    assertThat(span.getAttributes().get(AttributeKey.booleanKey("search.filter.text"))).isTrue();
    assertThat(span.getAttributes().get(AttributeKey.booleanKey("search.filter.city"))).isTrue();
    assertThat(span.getAttributes().get(AttributeKey.booleanKey("search.filter.dates"))).isTrue();
    assertThat(span.getAttributes().get(AttributeKey.booleanKey("search.filter.availability")))
        .isFalse();
    assertThat(span.getAttributes().get(AttributeKey.longKey("search.page"))).isEqualTo(2);
    assertThat(span.getAttributes().get(AttributeKey.longKey("search.page_size"))).isEqualTo(5);
    assertThat(span.getAttributes().get(AttributeKey.longKey("search.results"))).isEqualTo(2);
    assertThat(span.getAttributes().get(AttributeKey.longKey("search.total"))).isEqualTo(12);
    assertThat(span.getAttributes().asMap().values())
        .extracting(String::valueOf)
        .noneMatch(value -> value.contains("secret") || value.contains("Palermo"));
    assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
  }

  @Test
  void countsOnlyCompletedSearchesByActivityType() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ZERO);

    telemetry.traceSearch(
        filter(null, null, null, null, null), FIRST_PAGE, () -> page(List.of(), 0));
    telemetry.traceSearch(
        filter(ActivityType.INDOOR, null, null, null, null), FIRST_PAGE, () -> page(List.of(), 0));
    assertThatThrownBy(
            () ->
                telemetry.traceSearch(
                    filter(ActivityType.INDOOR, null, null, null, null),
                    FIRST_PAGE,
                    () -> {
                      throw new IllegalStateException("database down");
                    }))
        .isInstanceOf(IllegalStateException.class);

    List<LongPointData> points = counterPoints();
    assertThat(points)
        .extracting(point -> point.getAttributes().get(ActivitySearchTelemetry.FILTER_TYPE))
        .containsExactlyInAnyOrder("any", "INDOOR");
    assertThat(points).allSatisfy(point -> assertThat(point.getValue()).isEqualTo(1));
  }

  @Test
  void aFailedSearchMarksItsSpanAsAnErrorAndRethrows() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ZERO);
    IllegalStateException failure = new IllegalStateException("database down");

    assertThatThrownBy(
            () ->
                telemetry.traceSearch(
                    filter(null, null, null, null, null),
                    FIRST_PAGE,
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);

    SpanData span = onlySpan(ActivitySearchTelemetry.SEARCH_SPAN);
    assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
    assertThat(span.getAttributes().get(AttributeKey.stringKey("error.type")))
        .isEqualTo("IllegalStateException");
    assertThat(span.getEvents()).extracting("name").contains("exception");
    assertThat(counterPoints()).isEmpty();
  }

  @Test
  void timesEachDatabaseAccessByOperationEvenWhenItFails() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ZERO);

    telemetry.timeDatabase("count", () -> 3L);
    telemetry.timeDatabase("find", List::of);
    assertThatThrownBy(
            () ->
                telemetry.timeDatabase(
                    "find",
                    () -> {
                      throw new IllegalStateException("timeout");
                    }))
        .isInstanceOf(IllegalStateException.class);

    List<HistogramPointData> points = histogramPoints();
    assertThat(points)
        .extracting(point -> point.getAttributes().get(ActivitySearchTelemetry.DB_OPERATION))
        .containsExactlyInAnyOrder("count", "find");
    assertThat(pointFor(points, "count").getCount()).isEqualTo(1);
    assertThat(pointFor(points, "find").getCount()).isEqualTo(2);
  }

  @Test
  void recordsWhereTheTotalCameFromOnTheSearchSpan() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ZERO);

    telemetry.traceSearch(
        filter(null, null, null, null, null),
        FIRST_PAGE,
        () -> {
          telemetry.recordTotalSource("cache");
          return page(List.of(), 0);
        });

    assertThat(
            onlySpan(ActivitySearchTelemetry.SEARCH_SPAN)
                .getAttributes()
                .get(ActivitySearchTelemetry.TOTAL_SOURCE))
        .isEqualTo("cache");
  }

  @Test
  void aConfiguredDelayRunsAsItsOwnChildSpan() {
    ActivitySearchTelemetry telemetry = telemetry(Duration.ofMillis(30));

    telemetry.traceSearch(
        filter(null, null, null, null, null), FIRST_PAGE, () -> page(List.of(), 0));

    SpanData search = onlySpan(ActivitySearchTelemetry.SEARCH_SPAN);
    SpanData delay = onlySpan(ActivitySearchTelemetry.DELAY_SPAN);
    assertThat(delay.getParentSpanId()).isEqualTo(search.getSpanId());
    assertThat(delay.getEndEpochNanos() - delay.getStartEpochNanos())
        .isGreaterThanOrEqualTo(Duration.ofMillis(30).toNanos());
  }

  @Test
  void withoutADelayThereIsNoDelaySpan() {
    telemetry(Duration.ZERO)
        .traceSearch(filter(null, null, null, null, null), FIRST_PAGE, () -> page(List.of(), 0));

    assertThat(spans.getFinishedSpanItems())
        .extracting(SpanData::getName)
        .containsExactly(ActivitySearchTelemetry.SEARCH_SPAN);
  }

  @Test
  void anInvalidDateRangeIsAFailedSearchInTheService() {
    ActivityService service =
        new ActivityService(
            new InMemoryActivityRepository(),
            WeatherGateway.mainOnly(mock(IWeatherAdapter.class)),
            new InMemoryUserRepository(),
            mock(StatisticsEventRecorder.class),
            new InMemoryVotationRepository(),
            new NoOpImageStorage(),
            telemetry(Duration.ZERO));
    ActivityFilterDTO inverted =
        filter(
            null,
            null,
            null,
            LocalDateTime.of(2026, 9, 10, 0, 0),
            LocalDateTime.of(2026, 9, 1, 0, 0));

    assertThatThrownBy(() -> service.search(inverted, FIRST_PAGE))
        .isInstanceOf(InvalidActivityException.class);

    SpanData span = onlySpan(ActivitySearchTelemetry.SEARCH_SPAN);
    assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
    assertThat(counterPoints()).isEmpty();
  }

  @Test
  void aSuccessfulSearchInTheServiceIsCounted() {
    ActivityService service =
        new ActivityService(
            new InMemoryActivityRepository(),
            WeatherGateway.mainOnly(mock(IWeatherAdapter.class)),
            new InMemoryUserRepository(),
            mock(StatisticsEventRecorder.class),
            new InMemoryVotationRepository(),
            new NoOpImageStorage(),
            telemetry(Duration.ZERO));

    service.search(filter(null, null, null, null, null), FIRST_PAGE);

    assertThat(onlySpan(ActivitySearchTelemetry.SEARCH_SPAN).getStatus().getStatusCode())
        .isEqualTo(StatusCode.UNSET);
    assertThat(counterPoints()).singleElement().extracting(LongPointData::getValue).isEqualTo(1L);
  }

  private ActivitySearchTelemetry telemetry(Duration delay) {
    return new ActivitySearchTelemetry(sdk, delay);
  }

  private static ActivityFilterDTO filter(
      ActivityType type, String city, String title, LocalDateTime dateFrom, LocalDateTime dateTo) {
    return new ActivityFilterDTO(type, city, dateFrom, dateTo, null, List.of(), title, List.of());
  }

  private static PageResponse<String> page(List<String> content, long total) {
    return new PageResponse<>(content, 0, 12, total, 1, true, true);
  }

  private SpanData onlySpan(String name) {
    return spans.getFinishedSpanItems().stream()
        .filter(span -> span.getName().equals(name))
        .reduce(
            (first, second) -> {
              throw new AssertionError("More than one span named " + name);
            })
        .orElseThrow(() -> new AssertionError("No span named " + name));
  }

  private List<LongPointData> counterPoints() {
    return metric(ActivitySearchTelemetry.SEARCHES_COMPLETED)
        .map(data -> List.copyOf(data.getLongSumData().getPoints()))
        .orElse(List.of());
  }

  private List<HistogramPointData> histogramPoints() {
    return metric(ActivitySearchTelemetry.DB_DURATION)
        .map(data -> List.copyOf(data.getHistogramData().getPoints()))
        .orElse(List.of());
  }

  private Optional<MetricData> metric(String name) {
    return metrics.collectAllMetrics().stream()
        .filter(data -> data.getName().equals(name))
        .findFirst();
  }

  private static HistogramPointData pointFor(List<HistogramPointData> points, String operation) {
    return points.stream()
        .filter(
            point ->
                operation.equals(point.getAttributes().get(ActivitySearchTelemetry.DB_OPERATION)))
        .findFirst()
        .orElseThrow();
  }
}
