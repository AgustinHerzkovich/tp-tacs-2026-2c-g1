package com.solnotfound.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.adapters.legacy.LegacyProviderUnavailableException;
import com.solnotfound.adapters.legacy.LegacyWeatherProperties;
import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.user.WeatherProvider;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.ErrorCode;
import com.solnotfound.exception.WeatherUnavailableException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Answers weather questions about an activity with the provider a user chose, and keeps the
 * application usable when that provider is the unreliable legacy one.
 *
 * <p>For users of the legacy provider every lookup is tried there first. When it cannot give a
 * usable forecast (failure, open circuit, rate limit, overload, or the user's own request quota is
 * spent) the main provider answers instead and the result is marked as a fallback, so the screen
 * can tell the user. The same horizon as the main provider applies: the legacy service invents a
 * forecast for any date, including past ones, so dates outside the forecast window are rejected
 * before asking it.
 */
@Slf4j
@Service
public class WeatherGateway {

  /** Days ahead the platform accepts forecasts for, the same window as the main provider. */
  private static final int FORECAST_HORIZON_DAYS = 15;

  private final IWeatherAdapter mainProvider;
  private final LegacyWeatherProvider legacyProvider;
  private final StatisticsEventRecorder statisticsRecorder;
  private final RateLimiterConfig userLimit;
  private final Cache<String, RateLimiter> userLimiters =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).maximumSize(50_000).build();

  @Autowired
  public WeatherGateway(
      IWeatherAdapter mainProvider,
      LegacyWeatherProvider legacyProvider,
      StatisticsEventRecorder statisticsRecorder,
      LegacyWeatherProperties properties) {
    this.mainProvider = mainProvider;
    this.legacyProvider = legacyProvider;
    this.statisticsRecorder = statisticsRecorder;
    this.userLimit =
        RateLimiterConfig.custom()
            .limitForPeriod(Math.max(1, properties.userRequestsPerMinute()))
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ZERO)
            .build();
  }

  /** A gateway without the legacy provider: legacy users always fall back to the main one. */
  public static WeatherGateway mainOnly(IWeatherAdapter mainProvider) {
    return new WeatherGateway(mainProvider, null, null, (RateLimiterConfig) null);
  }

  private WeatherGateway(
      IWeatherAdapter mainProvider,
      LegacyWeatherProvider legacyProvider,
      StatisticsEventRecorder statisticsRecorder,
      RateLimiterConfig userLimit) {
    this.mainProvider = mainProvider;
    this.legacyProvider = legacyProvider;
    this.statisticsRecorder = statisticsRecorder;
    this.userLimit = userLimit;
  }

  /**
   * Current weather at the activity's place.
   *
   * @param activity activity whose place is used
   * @param provider provider chosen by the user the answer is for
   * @param requestedBy user whose quota is spent, or {@code null} for background work
   * @return the reading and which provider produced it
   */
  public WeatherReading current(Activity activity, WeatherProvider provider, String requestedBy) {
    if (shouldTryLegacy(provider)) {
      try {
        return legacy(
            legacyProvider.forecast(
                activity.getLocation(),
                activity.now(),
                zoneOf(activity),
                () -> withinUserQuota(requestedBy)));
      } catch (LegacyProviderUnavailableException exception) {
        return fallback(mainProvider.getWeather(activity.getLocation()), exception);
      }
    }
    return main(mainProvider.getWeather(activity.getLocation()), provider);
  }

  /**
   * Forecast for one local time at the activity's place.
   *
   * @param activity activity whose place and zone are used
   * @param dateTime wall-clock time in the activity's zone
   * @param provider provider chosen by the user the answer is for
   * @param requestedBy user whose quota is spent, or {@code null} for background work
   * @return the reading and which provider produced it
   * @throws WeatherUnavailableException when no provider can answer, or the date is outside the
   *     forecast window
   */
  public WeatherReading forecast(
      Activity activity, LocalDateTime dateTime, WeatherProvider provider, String requestedBy) {
    if (shouldTryLegacy(provider)) {
      requireWithinHorizon(activity, List.of(dateTime));
      try {
        return legacy(
            legacyProvider.forecast(
                activity.getLocation(),
                dateTime,
                zoneOf(activity),
                () -> withinUserQuota(requestedBy)));
      } catch (LegacyProviderUnavailableException exception) {
        return fallback(mainProvider.getFutureClimate(activity.getLocation(), dateTime), exception);
      }
    }
    return main(mainProvider.getFutureClimate(activity.getLocation(), dateTime), provider);
  }

  /**
   * Forecasts for several local times at the activity's place, all from the same provider.
   *
   * @param activity activity whose place and zone are used
   * @param dateTimes wall-clock times in the activity's zone
   * @param provider provider chosen by the activity's organizer
   * @return forecasts in the same order as {@code dateTimes}
   * @throws WeatherUnavailableException when no provider can answer
   */
  public List<WeatherForecast> forecastRange(
      Activity activity, List<LocalDateTime> dateTimes, WeatherProvider provider) {
    if (dateTimes.isEmpty()) {
      return List.of();
    }
    if (shouldTryLegacy(provider)) {
      requireWithinHorizon(activity, dateTimes);
      try {
        return legacyProvider.forecastRange(activity.getLocation(), dateTimes, zoneOf(activity));
      } catch (LegacyProviderUnavailableException exception) {
        fallback(null, exception);
      }
    }
    return mainProvider.getForecastRange(activity.getLocation(), dateTimes);
  }

  private boolean shouldTryLegacy(WeatherProvider provider) {
    return provider == WeatherProvider.LEGACY && legacyProvider != null;
  }

  /**
   * Spends one lookup of the user's quota. Only lookups that would reach the legacy service ask for
   * it; cached forecasts do not. Background work ({@code null} user) has no quota.
   */
  private boolean withinUserQuota(String requestedBy) {
    if (requestedBy == null || userLimiter(requestedBy).acquirePermission()) {
      return true;
    }
    statisticsRecorder.recordProviderRefusal(LegacyWeatherProvider.PROVIDER, "USER_RATE_LIMITED");
    log.info("Legacy weather quota spent, using the main provider: userId={}", requestedBy);
    return false;
  }

  private RateLimiter userLimiter(String userId) {
    return userLimiters.get(userId, key -> RateLimiter.of("legacy-user-" + key, userLimit));
  }

  private WeatherReading legacy(WeatherForecast forecast) {
    return new WeatherReading(forecast, WeatherProvider.LEGACY, false);
  }

  private WeatherReading main(WeatherForecast forecast, WeatherProvider requested) {
    // A legacy user served by the main provider without trying legacy (no legacy configured, or
    // quota spent) still gets the fallback flag, so the screen can say so.
    return new WeatherReading(
        forecast, WeatherProvider.OPEN_METEO, requested == WeatherProvider.LEGACY);
  }

  private WeatherReading fallback(
      WeatherForecast forecast, LegacyProviderUnavailableException cause) {
    if (statisticsRecorder != null) {
      statisticsRecorder.recordFallback(LegacyWeatherProvider.PROVIDER);
    }
    log.info("Legacy weather unavailable, using the main provider: {}", cause.getMessage());
    return new WeatherReading(forecast, WeatherProvider.OPEN_METEO, true);
  }

  private void requireWithinHorizon(Activity activity, List<LocalDateTime> dateTimes) {
    LocalDate today = activity.now().toLocalDate();
    for (LocalDateTime dateTime : dateTimes) {
      LocalDate date = dateTime.toLocalDate();
      if (date.isBefore(today)) {
        throw new WeatherUnavailableException("Forecast is not available for past dates");
      }
      if (date.isAfter(today.plusDays(FORECAST_HORIZON_DAYS))) {
        throw new WeatherUnavailableException(
            ErrorCode.FORECAST_NOT_YET_AVAILABLE, "Forecast is outside the forecast window");
      }
    }
  }

  private static ZoneId zoneOf(Activity activity) {
    return activity.getTimeZone() == null
        ? ZoneId.systemDefault()
        : ZoneId.of(activity.getTimeZone());
  }

  /**
   * A forecast and where it came from.
   *
   * @param fallback {@code true} when the user chose the legacy provider but the main one answered
   */
  public record WeatherReading(
      WeatherForecast forecast, WeatherProvider servedBy, boolean fallback) {}
}
