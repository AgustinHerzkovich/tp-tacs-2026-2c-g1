package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityStatusSchedulerTest {

  @Mock private IActivityRepository activityRepository;

  private ActivityStatusScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new ActivityStatusScheduler(activityRepository);
  }

  @Test
  void marksPastActiveActivityAsFinished() {
    Activity activity = activityAt(LocalDateTime.now().minusMinutes(1));
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.FINISHED);
    verify(activityRepository).save(activity);
  }

  @Test
  void leavesFutureActivityActive() {
    Activity activity = activityAt(LocalDateTime.now().plusHours(1));
    when(activityRepository.findActive()).thenReturn(List.of(activity));

    scheduler.finishPastActivities();

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CONFIRMED);
    verify(activityRepository, never()).save(activity);
  }

  private Activity activityAt(LocalDateTime dateTime) {
    Activity activity = new Activity();
    activity.setId("activity-id");
    activity.setDateTime(dateTime);
    return activity;
  }
}
