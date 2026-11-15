package com.solnotfound.observability;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.PageResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Manual OpenTelemetry instrumentation of the activity search. The Java agent already traces the
 * HTTP request and every MongoDB command; this class adds what only the application knows: the
 * business operation as a span with the filters used, a counter of completed searches and the
 * duration of the database accesses that belong to a search.
 *
 * <p>Attributes never carry the searched text, identifiers or user data: filters are recorded only
 * as "used or not", and the metric attributes take a handful of fixed values.
 */
@Component
public class ActivitySearchTelemetry {

  static final String SCOPE = "com.solnotfound.activity-search";
  static final String SEARCH_SPAN = "activities.search";
  static final String DELAY_SPAN = "activities.search.simulated_delay";
  static final String SEARCHES_COMPLETED = "planazo.activities.search.completed";
  static final String DB_DURATION = "planazo.activities.search.db.duration";

  static final AttributeKey<String> FILTER_TYPE = AttributeKey.stringKey("search.filter.type");
  static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation.name");
  private static final AttributeKey<Boolean> FILTER_TEXT =
      AttributeKey.booleanKey("search.filter.text");
  private static final AttributeKey<Boolean> FILTER_CITY =
      AttributeKey.booleanKey("search.filter.city");
  private static final AttributeKey<Boolean> FILTER_DATES =
      AttributeKey.booleanKey("search.filter.dates");
  private static final AttributeKey<Boolean> FILTER_AVAILABILITY =
      AttributeKey.booleanKey("search.filter.availability");
  private static final AttributeKey<Boolean> FILTER_STATUS =
      AttributeKey.booleanKey("search.filter.status");
  private static final AttributeKey<Boolean> FILTER_IDS =
      AttributeKey.booleanKey("search.filter.ids");
  private static final AttributeKey<Long> PAGE = AttributeKey.longKey("search.page");
  private static final AttributeKey<Long> PAGE_SIZE = AttributeKey.longKey("search.page_size");
  private static final AttributeKey<Long> RESULTS = AttributeKey.longKey("search.results");
  private static final AttributeKey<Long> TOTAL = AttributeKey.longKey("search.total");
  private static final AttributeKey<Long> DELAY_MS = AttributeKey.longKey("search.delay_ms");
  static final AttributeKey<String> TOTAL_SOURCE = AttributeKey.stringKey("search.total_source");
  private static final AttributeKey<String> DB_COLLECTION =
      AttributeKey.stringKey("db.collection.name");
  private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
  private static final String ANY_TYPE = "any";
  private static final String COLLECTION = "activities";
  private static final List<Double> DB_BUCKETS_SECONDS =
      List.of(0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0);

  private final Tracer tracer;
  private final LongCounter searchesCompleted;
  private final DoubleHistogram dbDuration;
  private final Duration simulatedDelay;

  @Autowired
  public ActivitySearchTelemetry(
      OpenTelemetry openTelemetry,
      @Value("${observability.search.delay:0ms}") Duration simulatedDelay) {
    this.tracer = openTelemetry.getTracer(SCOPE);
    this.searchesCompleted =
        openTelemetry
            .getMeter(SCOPE)
            .counterBuilder(SEARCHES_COMPLETED)
            .setUnit("{search}")
            .setDescription("Activity searches that returned a page of results")
            .build();
    this.dbDuration =
        openTelemetry
            .getMeter(SCOPE)
            .histogramBuilder(DB_DURATION)
            .setUnit("s")
            .setDescription("Duration of each MongoDB access made by an activity search")
            .setExplicitBucketBoundariesAdvice(DB_BUCKETS_SECONDS)
            .build();
    this.simulatedDelay = simulatedDelay;
  }

  public static ActivitySearchTelemetry noop() {
    return new ActivitySearchTelemetry(OpenTelemetry.noop(), Duration.ZERO);
  }

  /**
   * Runs a search inside the {@value #SEARCH_SPAN} span, which stays current so the database spans
   * of the Java agent become its children. The span records which filters were used, the requested
   * page and, on success, how many results it returned. A successful search also increments {@value
   * #SEARCHES_COMPLETED}; a failing one marks the span as an error, records the exception and
   * rethrows it unchanged. When a simulated delay is configured it runs first, as its own child
   * span, so a degradation introduced on purpose is told apart in the trace.
   *
   * @param filter criteria of the search
   * @param pageable requested page
   * @param search the actual search
   * @return the page the search returned
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
      justification = "The search's own failure is rethrown unchanged after being recorded")
  public <T> PageResponse<T> traceSearch(
      ActivityFilterDTO filter, Pageable pageable, Supplier<PageResponse<T>> search) {
    String type = filter.type() == null ? ANY_TYPE : filter.type().name();
    Span span =
        tracer
            .spanBuilder(SEARCH_SPAN)
            .setAttribute(FILTER_TYPE, type)
            .setAttribute(FILTER_TEXT, hasText(filter.title()))
            .setAttribute(FILTER_CITY, hasText(filter.city()))
            .setAttribute(FILTER_DATES, filter.dateFrom() != null || filter.dateTo() != null)
            .setAttribute(FILTER_AVAILABILITY, filter.availability() != null)
            .setAttribute(FILTER_STATUS, !filter.statuses().isEmpty())
            .setAttribute(FILTER_IDS, !filter.ids().isEmpty())
            .setAttribute(PAGE, (long) pageable.getPageNumber())
            .setAttribute(PAGE_SIZE, (long) pageable.getPageSize())
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      simulateDelay();
      PageResponse<T> page = search.get();
      span.setAttribute(RESULTS, (long) page.content().size());
      span.setAttribute(TOTAL, page.totalElements());
      searchesCompleted.add(1, Attributes.of(FILTER_TYPE, type));
      return page;
    } catch (RuntimeException exception) {
      span.recordException(exception);
      span.setAttribute(ERROR_TYPE, exception.getClass().getSimpleName());
      span.setStatus(StatusCode.ERROR, exception.getClass().getSimpleName());
      throw exception;
    } finally {
      span.end();
    }
  }

  /**
   * Times one MongoDB access of a search into {@value #DB_DURATION}, tagged with the operation
   * ({@code find} for the page, {@code find_people} for its participants and organizers, {@code
   * count} for the total). Failed accesses are timed too, so a slow failure still shows.
   *
   * @param operation database operation, one of a fixed set of names
   * @param access the database access
   * @return what the access returned
   */
  public <T> T timeDatabase(String operation, Supplier<T> access) {
    long startedAt = System.nanoTime();
    try {
      return access.get();
    } finally {
      dbDuration.record(
          (System.nanoTime() - startedAt) / 1e9,
          Attributes.of(DB_OPERATION, operation, DB_COLLECTION, COLLECTION));
    }
  }

  /**
   * Notes on the current search span where the total came from: {@code count} when it was counted
   * in MongoDB, {@code cache} when a recent count was reused. A search whose page already told the
   * total records nothing.
   *
   * @param source one of a fixed set of names
   */
  public void recordTotalSource(String source) {
    Span.current().setAttribute(TOTAL_SOURCE, source);
  }

  private void simulateDelay() {
    if (simulatedDelay.isZero() || simulatedDelay.isNegative()) {
      return;
    }
    Span delay =
        tracer
            .spanBuilder(DELAY_SPAN)
            .setAttribute(DELAY_MS, simulatedDelay.toMillis())
            .startSpan();
    try {
      Thread.sleep(simulatedDelay.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    } finally {
      delay.end();
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
