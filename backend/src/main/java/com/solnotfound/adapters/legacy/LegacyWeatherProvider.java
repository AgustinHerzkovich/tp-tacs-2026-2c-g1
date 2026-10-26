package com.solnotfound.adapters.legacy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.service.StatisticsEventRecorder;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * The legacy weather service as the rest of the backend sees it: a forecast for a place and hour,
 * with every protection the unreliable service needs.
 *
 * <p>A lookup goes, from the outside in, through:
 *
 * <ol>
 *   <li>a cache of valid forecasts: the service answers the same for the same place and hour, so a
 *       repeated question never reaches it;
 *   <li>a circuit breaker that stops calling the service while it keeps failing even after retries;
 *   <li>a retry with exponential backoff and jitter, only for failures that are the provider's
 *       fault;
 *   <li>a rate limiter that caps the calls per second the platform sends to the service;
 *   <li>a bulkhead that caps concurrent calls: under concurrency the service degrades sharply, so
 *       the excess is turned away immediately instead of piling up (back pressure);
 *   <li>a single call with connect and read timeouts, whose answer is fully validated.
 * </ol>
 *
 * <p>Any refusal or failure surfaces as {@link LegacyProviderUnavailableException}; the caller
 * decides the fallback. Every attempt and every refusal is recorded as a statistics event.
 */
@Slf4j
public class LegacyWeatherProvider {

  public static final String PROVIDER = "Legacy";

  private final LegacyWeatherClient client;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final RateLimiter rateLimiter;
  private final Bulkhead bulkhead;
  private final StatisticsEventRecorder statisticsRecorder;
  private final Cache<String, CachedForecast> cache;

  public LegacyWeatherProvider(
      LegacyWeatherClient client,
      CircuitBreaker circuitBreaker,
      Retry retry,
      RateLimiter rateLimiter,
      Bulkhead bulkhead,
      StatisticsEventRecorder statisticsRecorder,
      LegacyWeatherProperties properties) {
    this.client = client;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
    this.rateLimiter = rateLimiter;
    this.bulkhead = bulkhead;
    this.statisticsRecorder = statisticsRecorder;
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(properties.cacheTtl())
            .maximumSize(properties.cacheMaximumSize())
            .recordStats()
            .build();
  }

  public WeatherForecast forecast(Location location, LocalDateTime dateTime, ZoneId zone) {
    return forecast(location, dateTime, zone, () -> true);
  }

  /**
   * Returns the forecast for a place at a local time, from the cache or from the service. {@code
   * mayCall} is asked only on a cache miss, so a quota only counts the lookups that really reach
   * the provider.
   *
   * @param location place of the activity; it must have coordinates, the service takes no names
   * @param dateTime wall-clock time in {@code zone}
   * @param zone zone of the activity, used to turn {@code dateTime} into an instant
   * @param mayCall whether this lookup may call the service
   * @return the validated forecast, timed at {@code dateTime}
   * @throws LegacyProviderUnavailableException when no usable forecast could be obtained or {@code
   *     mayCall} refused the call
   */
  public WeatherForecast forecast(
      Location location, LocalDateTime dateTime, ZoneId zone, BooleanSupplier mayCall) {
    if (location.latitude() == null || location.longitude() == null) {
      throw new LegacyProviderUnavailableException("Legacy provider needs coordinates");
    }
    double latitude = location.latitude();
    double longitude = location.longitude();
    Instant hour = dateTime.atZone(zone).toInstant().truncatedTo(ChronoUnit.HOURS);
    String key = String.format(Locale.ROOT, "%.2f|%.2f|%s", latitude, longitude, hour);

    CachedForecast cached = cache.getIfPresent(key);
    if (cached == null) {
      if (!mayCall.getAsBoolean()) {
        throw new LegacyProviderUnavailableException("Legacy lookup quota spent");
      }
      WeatherForecast fetched = call(() -> client.forecast(latitude, longitude, hour, zone));
      cached = CachedForecast.of(fetched);
      cache.put(key, cached);
    }
    return cached.at(dateTime);
  }

  /**
   * Returns the forecast for several local times at one place. The service has no range query, so
   * each hour is a separate lookup; repeated hours are served by the cache.
   *
   * @param location place of the activity
   * @param dateTimes wall-clock times in {@code zone}
   * @param zone zone of the activity
   * @return forecasts in the same order as {@code dateTimes}
   * @throws LegacyProviderUnavailableException when any of them could not be obtained
   */
  public List<WeatherForecast> forecastRange(
      Location location, List<LocalDateTime> dateTimes, ZoneId zone) {
    return dateTimes.stream().map(dateTime -> forecast(location, dateTime, zone)).toList();
  }

  public CacheStats cacheStats() {
    return cache.stats();
  }

  public CircuitBreaker.State circuitBreakerState() {
    return circuitBreaker.getState();
  }

  /** Failure percentage of the circuit breaker window, negative while there are too few calls. */
  public float failureRate() {
    return circuitBreaker.getMetrics().getFailureRate();
  }

  public boolean isAlive() {
    return client.isAlive();
  }

  private WeatherForecast call(Supplier<WeatherForecast> request) {
    // Innermost first: each attempt takes a bulkhead slot and a rate limiter permit; the retry
    // repeats attempts; the circuit breaker sees the outcome of the whole retried lookup.
    Supplier<WeatherForecast> decorated = () -> recorded(request);
    decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
    decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
    decorated = Retry.decorateSupplier(retry, decorated);
    decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
    try {
      return decorated.get();
    } catch (CallNotPermittedException exception) {
      statisticsRecorder.recordProviderRefusal(PROVIDER, "CIRCUIT_OPEN");
      throw new LegacyProviderUnavailableException("Legacy circuit breaker is open", exception);
    } catch (BulkheadFullException exception) {
      statisticsRecorder.recordProviderRefusal(PROVIDER, "OVERLOADED");
      throw new LegacyProviderUnavailableException("Legacy provider is at capacity", exception);
    } catch (RequestNotPermitted exception) {
      statisticsRecorder.recordProviderRefusal(PROVIDER, "RATE_LIMITED");
      throw new LegacyProviderUnavailableException("Legacy provider rate limit reached", exception);
    } catch (LegacyProviderException exception) {
      throw new LegacyProviderUnavailableException(exception.getMessage(), exception);
    } catch (RuntimeException exception) {
      // Anything else (an invalid header, a failure of the identity token library) must still end
      // in the fallback: the weather never fails because of the legacy provider.
      log.warn("Unexpected failure calling the legacy weather service", exception);
      throw new LegacyProviderUnavailableException("Unexpected legacy provider failure", exception);
    }
  }

  private WeatherForecast recorded(Supplier<WeatherForecast> request) {
    long startedAt = System.nanoTime();
    try {
      WeatherForecast forecast = request.get();
      statisticsRecorder.recordProviderCall(PROVIDER, true, elapsedMs(startedAt), null);
      return forecast;
    } catch (LegacyProviderException exception) {
      statisticsRecorder.recordProviderCall(
          PROVIDER, false, elapsedMs(startedAt), exception.getFailure().name());
      throw exception;
    }
  }

  private static long elapsedMs(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }

  /** A cached forecast without its local time, so it can be served to any zone. */
  private record CachedForecast(Float temperature, Float chanceOfRain, Float windSpeed) {
    static CachedForecast of(WeatherForecast forecast) {
      return new CachedForecast(
          forecast.getTemperature(), forecast.getChanceOfRain(), forecast.getWindSpeed());
    }

    WeatherForecast at(LocalDateTime dateTime) {
      return new WeatherForecast(dateTime, temperature, chanceOfRain, windSpeed);
    }
  }
}
