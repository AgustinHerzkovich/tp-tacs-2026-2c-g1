package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.StartingSoonNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ActivityStatusSchedulerTest {

  private IActivityRepository activityRepository;
  private ApplicationEventPublisher eventPublisher;
  private ActivityStatusTransitionService transitionService;
  private ActivityStatusScheduler scheduler;

  @BeforeEach
  void setUp() {
    activityRepository = mock(IActivityRepository.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    transitionService =
        new ActivityStatusTransitionService(
            activityRepository, eventPublisher, mock(StatisticsEventRecorder.class));
    scheduler =
        new ActivityStatusScheduler(
            activityRepository, eventPublisher, transitionService, Duration.ofMinutes(90));
  }

  @Test
  void marksPastActiveActivityAsFinished() {
    Activity activity = activityAt(LocalDateTime.now().minusMinutes(1));
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.FINISHED);
    verify(activityRepository).save(activity);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void usesTheActivityZoneInsteadOfTheServerClockToFinishActivities() {
    // Kiritimati (UTC+14) and Pago Pago (UTC-11): wherever the JVM runs, at least one is far away.
    // An activity still ahead in its own zone must not be finished because the server clock
    // already passed that wall-clock time, and one that already started there must be.
    for (String zone : new String[] {"Pacific/Kiritimati", "Pacific/Pago_Pago"}) {
      LocalDateTime zoneNow = LocalDateTime.now(java.time.ZoneId.of(zone));
      Activity upcoming = activityAt(zoneNow.plusHours(3));
      upcoming.setTimeZone(zone);
      Activity past = activityAt(zoneNow.minusMinutes(30));
      past.setTimeZone(zone);
      when(activityRepository.findActive()).thenReturn(List.of(upcoming, past));

      scheduler.finishPastActivities();

      assertThat(upcoming.getStatus()).as(zone).isEqualTo(ActivityStatus.CONFIRMED);
      assertThat(past.getStatus()).as(zone).isEqualTo(ActivityStatus.FINISHED);
    }
  }

  @Test
  void leavesProposedActivityForItsVotationEvenIfItsDateHasPassed() {
    Activity activity = activityAt(LocalDateTime.now().minusMinutes(1));
    activity.setStatus(ActivityStatus.PROPOSED);
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PROPOSED);
    verify(activityRepository, never()).save(any());
  }

  @Test
  void doesNotSendStartingSoonNoticeForProposedActivity() {
    Activity activity = activityAt(LocalDateTime.now().plusMinutes(60));
    activity.setStatus(ActivityStatus.PROPOSED);
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();

    verify(eventPublisher, never()).publishEvent(any());
    assertThat(activity.wasStartingSoonNotificationSent()).isFalse();
  }

  @Test
  void notifiesOnceWhenActivityIsInsideThreshold() {
    Activity activity = activityAt(LocalDateTime.now().plusMinutes(60));
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();
    scheduler.finishPastActivities();

    ArgumentCaptor<ActivityNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().type()).isInstanceOf(StartingSoonNotificationType.class);
    assertThat(activity.wasStartingSoonNotificationSent()).isTrue();
  }

  @Test
  void includesExactThresholdBoundary() {
    LocalDateTime now = LocalDateTime.of(2026, 9, 1, 10, 0);
    Activity activity = activityAt(now.plusMinutes(90));

    assertThat(activity.nearStart(now, Duration.ofMinutes(90))).isTrue();
  }

  @Test
  void excludesActivityStartAndTimesOutsideThreshold() {
    LocalDateTime now = LocalDateTime.of(2026, 9, 1, 10, 0);

    assertThat(activityAt(now).nearStart(now, Duration.ofMinutes(90))).isFalse();
    assertThat(activityAt(now.plusMinutes(91)).nearStart(now, Duration.ofMinutes(90))).isFalse();
  }

  @Test
  void canNotifyAgainAfterRescheduling() {
    Activity activity = activityAt(LocalDateTime.now().plusMinutes(60));
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();
    activity.setDateTime(LocalDateTime.now().plusMinutes(70));
    scheduler.finishPastActivities();

    verify(eventPublisher, times(2)).publishEvent(any(ActivityNotificationEvent.class));
  }

  @Test
  void continuesWithRemainingActivitiesWhenOneNotificationFails() {
    Activity first = activityAt(LocalDateTime.now().plusMinutes(30));
    first.setId("first");
    Activity second = activityAt(LocalDateTime.now().plusMinutes(30));
    second.setId("second");
    when(activityRepository.findActive()).thenReturn(List.of(first, second));
    doThrow(new RuntimeException("notification failed"))
        .doNothing()
        .when(eventPublisher)
        .publishEvent(any(ActivityNotificationEvent.class));

    scheduler.finishPastActivities();

    verify(eventPublisher, times(2)).publishEvent(any(ActivityNotificationEvent.class));
    assertThat(first.wasStartingSoonNotificationSent()).isFalse();
    assertThat(second.wasStartingSoonNotificationSent()).isTrue();
  }

  private Activity activityAt(LocalDateTime dateTime) {
    Activity activity = new Activity();
    activity.setId("activity-id");
    activity.setDateTime(dateTime);
    return activity;
  }
}
