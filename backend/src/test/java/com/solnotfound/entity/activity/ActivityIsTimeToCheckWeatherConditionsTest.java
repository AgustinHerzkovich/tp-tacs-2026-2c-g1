package com.solnotfound.entity.activity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ActivityIsTimeToCheckWeatherConditionsTest {

  private Activity buildActivity(LocalDateTime dateTime, Integer anticipationWindow) {
    Activity activity = new Activity();
    activity.setDateTime(dateTime);
    activity.setAnticipationWindow(anticipationWindow);
    return activity;
  }

  @Test
  void returnsTrueWhenNowIsWithinTheAnticipationWindow() {
    // Activity starts in 1 hour, anticipation window is 2 hours
    // -> window opened 1 hour ago, so "now" falls inside it
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 2);

    assertThat(activity.isTimeToCheckWeatherConditions()).isTrue();
  }

  @Test
  void keepsReturningTrueInsideTheWindowSoTheForecastIsCheckedPeriodically() {
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 3);

    assertThat(activity.isTimeToCheckWeatherConditions()).isTrue();
    assertThat(activity.isTimeToCheckWeatherConditions()).isTrue();
  }

  @Test
  void returnsFalseWhenItIsTooEarlyToCheck() {
    // Activity starts in 3 hours, anticipation window is 2 hours
    // -> window opens in 1 hour, "now" is still too early
    Activity activity = buildActivity(LocalDateTime.now().plusHours(3), 2);

    assertThat(activity.isTimeToCheckWeatherConditions()).isFalse();
  }

  @Test
  void returnsFalseWhenTheActivityAlreadyStarted() {
    // Activity started 1 hour ago
    Activity activity = buildActivity(LocalDateTime.now().minusHours(1), 2);

    assertThat(activity.isTimeToCheckWeatherConditions()).isFalse();
  }

  @Test
  void returnsTrueRightAtTheStartOfTheAnticipationWindow() {
    // Activity starts in exactly the anticipation window size
    // -> now is (just barely) after window start due to execution time elapsed
    Activity activity = buildActivity(LocalDateTime.now().plusHours(2), 2);

    assertThat(activity.isTimeToCheckWeatherConditions()).isTrue();
  }

  @Test
  void returnsFalseWhenAnticipationWindowIsZeroAndActivityHasNotStartedYet() {
    // Window of 0 hours means the window opens exactly at dateTime,
    // so before dateTime it should always be false
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 0);

    assertThat(activity.isTimeToCheckWeatherConditions()).isFalse();
  }

  @Test
  void watchesARescheduledActivityAgainOnItsNewDate() {
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 2);
    activity.setStatus(ActivityStatus.PROPOSED);
    activity.setStatus(ActivityStatus.RESCHEDULED);

    assertThat(activity.isTimeToCheckWeatherConditions()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = ActivityStatus.class,
      names = {"PROPOSED", "CANCELLED", "FINISHED"})
  void returnsFalseWhenTheActivityIsNotGoingAheadOnItsCurrentDate(ActivityStatus status) {
    // PROPOSED: its votation is already choosing a new date.
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 2);
    activity.setStatus(status);

    assertThat(activity.isTimeToCheckWeatherConditions()).isFalse();
  }

  @Test
  void evaluatesTheWindowInTheActivityZoneInsteadOfTheServerClock() {
    // Kiritimati (UTC+14) and Pago Pago (UTC-11) are 25 hours apart, so at least one of them is far
    // from the JVM zone: reading "now" from the server clock would fail one of the two activities.
    for (String zone : new String[] {"Pacific/Kiritimati", "Pacific/Pago_Pago"}) {
      Activity activity = buildActivity(LocalDateTime.now(ZoneId.of(zone)).plusHours(1), 2);
      activity.setTimeZone(zone);

      assertThat(activity.isTimeToCheckWeatherConditions()).as(zone).isTrue();
      assertThat(activity.now()).as(zone).isBefore(activity.getDateTime());
    }
  }

  @Test
  void fallsBackToTheJvmZoneWhenTheActivityHasNoStoredZone() {
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 2);

    assertThat(activity.getTimeZone()).isNull();
    assertThat(activity.now()).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
  }
}
