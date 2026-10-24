package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.adapters.legacy.LegacyProviderUnavailableException;
import com.solnotfound.adapters.legacy.LegacyWeatherProperties;
import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.user.WeatherProvider;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.ErrorCode;
import com.solnotfound.exception.WeatherUnavailableException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeatherGatewayTest {

  private static final String ZONE = "America/Argentina/Buenos_Aires";

  private IWeatherAdapter main;
  private LegacyWeatherProvider legacy;
  private StatisticsEventRecorder statistics;
  private Activity activity;
  private LocalDateTime tomorrow;

  @BeforeEach
  void setUp() {
    main = mock(IWeatherAdapter.class);
    legacy = mock(LegacyWeatherProvider.class);
    statistics = mock(StatisticsEventRecorder.class);
    activity = new Activity();
    activity.setId("activity-1");
    activity.setTimeZone(ZONE);
    activity.setLocation(new Location(null, -34.6, -58.38));
    tomorrow = activity.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
    activity.setDateTime(tomorrow);
  }

  @Test
  void usersOfTheMainProviderNeverReachTheLegacyOne() {
    when(main.getFutureClimate(any(), any())).thenReturn(forecast(18f));

    var reading = gateway(30).forecast(activity, tomorrow, WeatherProvider.OPEN_METEO, "user-1");

    assertThat(reading.servedBy()).isEqualTo(WeatherProvider.OPEN_METEO);
    assertThat(reading.fallback()).isFalse();
    verify(legacy, never()).forecast(any(), any(), any());
  }

  @Test
  void usersOfTheLegacyProviderGetItsForecast() {
    when(legacy.forecast(any(), any(), any(), any())).thenReturn(forecast(25f));

    var reading = gateway(30).forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-1");

    assertThat(reading.forecast().getTemperature()).isEqualTo(25f);
    assertThat(reading.servedBy()).isEqualTo(WeatherProvider.LEGACY);
    assertThat(reading.fallback()).isFalse();
    verify(main, never()).getFutureClimate(any(), any());
  }

  @Test
  void fallsBackToTheMainProviderAndSaysSoWhenTheLegacyOneCannotAnswer() {
    when(legacy.forecast(any(), any(), any(), any()))
        .thenThrow(new LegacyProviderUnavailableException("circuit open"));
    when(main.getFutureClimate(any(), any())).thenReturn(forecast(18f));

    var reading = gateway(30).forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-1");

    assertThat(reading.forecast().getTemperature()).isEqualTo(18f);
    assertThat(reading.servedBy()).isEqualTo(WeatherProvider.OPEN_METEO);
    assertThat(reading.fallback()).isTrue();
    verify(statistics).recordFallback("Legacy");
  }

  @Test
  void aUserWhoSpentTheirQuotaIsServedByTheMainProvider() {
    when(legacy.forecast(any(), any(), any(), any())).thenAnswer(this::legacyAskingQuota);
    when(main.getFutureClimate(any(), any())).thenReturn(forecast(18f));
    WeatherGateway gateway = gateway(2);

    gateway.forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-1");
    gateway.forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-1");
    var third = gateway.forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-1");
    var otherUser = gateway.forecast(activity, tomorrow, WeatherProvider.LEGACY, "user-2");

    assertThat(third.fallback()).isTrue();
    assertThat(third.servedBy()).isEqualTo(WeatherProvider.OPEN_METEO);
    assertThat(otherUser.servedBy()).isEqualTo(WeatherProvider.LEGACY);
    verify(statistics).recordProviderRefusal("Legacy", "USER_RATE_LIMITED");
  }

  @Test
  void backgroundWorkIsNotLimitedByAnyUserQuota() {
    when(legacy.forecast(any(), any(), any(), any())).thenReturn(forecast(25f));
    WeatherGateway gateway = gateway(1);

    for (int index = 0; index < 5; index++) {
      assertThat(gateway.forecast(activity, tomorrow, WeatherProvider.LEGACY, null).servedBy())
          .isEqualTo(WeatherProvider.LEGACY);
    }
  }

  @Test
  void rejectsDatesBeyondTheForecastWindowBeforeAskingTheLegacyProvider() {
    LocalDateTime farAway = tomorrow.plusDays(30);

    assertThatThrownBy(
            () -> gateway(30).forecast(activity, farAway, WeatherProvider.LEGACY, "user-1"))
        .isInstanceOfSatisfying(
            WeatherUnavailableException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(ErrorCode.FORECAST_NOT_YET_AVAILABLE));
    verify(legacy, never()).forecast(any(), any(), any());
  }

  @Test
  void aRangeFallsBackAsAWholeSoAlternativesAreComparable() {
    List<LocalDateTime> range = List.of(tomorrow, tomorrow.plusHours(1));
    when(legacy.forecastRange(any(), any(), any()))
        .thenThrow(new LegacyProviderUnavailableException("overloaded"));
    when(main.getForecastRange(any(), any())).thenReturn(List.of(forecast(1f), forecast(2f)));

    var forecasts = gateway(30).forecastRange(activity, range, WeatherProvider.LEGACY);

    assertThat(forecasts).extracting(WeatherForecast::getTemperature).containsExactly(1f, 2f);
  }

  @Test
  void aGatewayWithoutALegacyProviderServesLegacyUsersFromTheMainOne() {
    when(main.getFutureClimate(any(), any())).thenReturn(forecast(18f));

    var reading =
        WeatherGateway.mainOnly(main).forecast(activity, tomorrow, WeatherProvider.LEGACY, "u");

    assertThat(reading.servedBy()).isEqualTo(WeatherProvider.OPEN_METEO);
    assertThat(reading.fallback()).isTrue();
  }

  /** Behaves like the real provider on a cache miss: asks the quota before calling. */
  private WeatherForecast legacyAskingQuota(org.mockito.invocation.InvocationOnMock invocation) {
    BooleanSupplier mayCall = invocation.getArgument(3);
    if (!mayCall.getAsBoolean()) {
      throw new LegacyProviderUnavailableException("quota");
    }
    return forecast(25f);
  }

  private WeatherGateway gateway(int requestsPerMinute) {
    LegacyWeatherProperties properties =
        new LegacyWeatherProperties(
            URI.create("http://legacy"),
            "token",
            null,
            Duration.ofMillis(500),
            Duration.ofMillis(500),
            Duration.ofHours(1),
            100,
            requestsPerMinute,
            null);
    return new WeatherGateway(main, legacy, statistics, properties);
  }

  private WeatherForecast forecast(float temperature) {
    return new WeatherForecast(tomorrow, temperature, 10f, 5f);
  }
}
