package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.CancelledNotificationType;
import com.solnotfound.entity.notification.ReprogrammedNotificationType;
import com.solnotfound.exception.InvalidActivityStatusTransitionException;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ActivityStatusTransitionServiceTest {

  private IActivityRepository repository;
  private ApplicationEventPublisher publisher;
  private ActivityStatusTransitionService service;
  private Activity activity;

  @BeforeEach
  void setUp() {
    repository = mock(IActivityRepository.class);
    publisher = mock(ApplicationEventPublisher.class);
    service = new ActivityStatusTransitionService(repository, publisher);
    activity = new Activity();
    activity.setId("activity-1");
  }

  @Test
  void cancelsAndPublishesNotification() {
    service.transition(activity, ActivityStatus.CANCELLED);

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
    assertNotificationType(CancelledNotificationType.class);
    verify(repository).save(activity);
  }

  @Test
  void reschedulesProposedActivityAndPublishesNotification() {
    activity.setStatus(ActivityStatus.PROPOSED);

    service.transition(activity, ActivityStatus.RESCHEDULED);

    assertNotificationType(ReprogrammedNotificationType.class);
  }

  @Test
  void finishesWithoutPublishingNotification() {
    service.transition(activity, ActivityStatus.FINISHED);

    verify(publisher, never()).publishEvent(any());
  }

  @Test
  void rejectsInvalidTransitionWithoutPersisting() {
    assertThatThrownBy(() -> service.transition(activity, ActivityStatus.RESCHEDULED))
        .isInstanceOf(InvalidActivityStatusTransitionException.class);

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CONFIRMED);
    verify(repository, never()).save(any());
  }

  private void assertNotificationType(Class<?> expectedType) {
    ArgumentCaptor<ActivityNotificationEvent> captor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(publisher).publishEvent(captor.capture());
    assertThat(captor.getValue().type()).isInstanceOf(expectedType);
  }
}
