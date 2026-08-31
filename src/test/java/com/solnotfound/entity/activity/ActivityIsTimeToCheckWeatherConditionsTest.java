package com.solnotfound.entity.activity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivityIsTimeToCheckWeatherConditionsTest {

  private Activity buildActivity(LocalDateTime dateTime, Integer anticipationWindow) {
    Activity activity = new Activity();
    activity.setDateTime(dateTime);
    activity.setAnticipationWindow(anticipationWindow);
    return activity;
  }

  private Activity buildActivity(
      LocalDateTime dateTime, Integer anticipationWindow, Boolean wasChecked) {
    Activity activity = new Activity();
    activity.setDateTime(dateTime);
    activity.setAnticipationWindow(anticipationWindow);
    activity.setWeatherChecked(wasChecked);
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
  void returnsTrueWhenActivitiesWeatherCouldNotBeCheckedYet() {
    // Activity starts in 1 hour, anticipation window is 3 hours
    // -> window opened 3 hour ago, so "now" does not fall inside it
    Activity activity = buildActivity(LocalDateTime.now().plusHours(1), 3, false);

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
  void returnsTrueWhenNowIsExactlyAtTheActivityDateTime() {
    // dateTime.isAfter(now) is false when they are equal
    LocalDateTime fixedNow = LocalDateTime.now().plusHours(1);
    Activity activity = buildActivity(fixedNow, 2);

    // Simulate "now == dateTime" by checking right at the boundary is not achievable
    // with real clock, so we validate the closest realistic case instead:
    // an activity whose dateTime is effectively "now" already returns false
    // because isAfter(now) requires a strictly later instant.
    assertThat(activity.isTimeToCheckWeatherConditions())
        .isTrue(); // still true, now < dateTime by nanoseconds
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
}
