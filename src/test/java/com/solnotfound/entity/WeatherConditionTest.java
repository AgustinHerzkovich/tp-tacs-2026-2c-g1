package com.solnotfound.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WeatherConditionTest {

  @Test
  void rainProbabilityIncludesConfiguredMaximum() {
    MaxRainProbabilityCondition condition = new MaxRainProbabilityCondition(30);

    assertThat(condition.isSatisfiedBy(forecast(20, 30, 10))).isTrue();
    assertThat(condition.isSatisfiedBy(forecast(20, 31, 10))).isFalse();
  }

  @Test
  void temperatureIncludesBothConfiguredLimits() {
    TemperatureRangeCondition condition = new TemperatureRangeCondition(10, 25);

    assertThat(condition.isSatisfiedBy(forecast(10, 0, 10))).isTrue();
    assertThat(condition.isSatisfiedBy(forecast(25, 0, 10))).isTrue();
    assertThat(condition.isSatisfiedBy(forecast(26, 0, 10))).isFalse();
  }

  @Test
  void windSpeedIncludesConfiguredMaximumInKilometersPerHour() {
    MaxWindCondition condition = new MaxWindCondition(25.0);

    assertThat(condition.isSatisfiedBy(forecast(20, 0, 25))).isTrue();
    assertThat(condition.isSatisfiedBy(forecast(20, 0, 26))).isFalse();
  }

  private WeatherForecast forecast(float temperature, float rain, float wind) {
    return new WeatherForecast(LocalDateTime.now(), temperature, rain, wind);
  }
}
