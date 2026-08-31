package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.CancelledNotificationType;
import com.solnotfound.entity.notification.ReprogrammedNotificationType;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.schedulers.VotationClosingScheduler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class VotationClosingSchedulerTest {

  private IVotationRepository votationRepository;
  private IActivityRepository activityRepository;
  private ApplicationEventPublisher eventPublisher;
  private VotationClosingScheduler scheduler;

  @BeforeEach
  void setUp() {
    votationRepository = mock(IVotationRepository.class);
    activityRepository = mock(IActivityRepository.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    ActivityStatusTransitionService transitionService =
        new ActivityStatusTransitionService(activityRepository, eventPublisher);
    scheduler = new VotationClosingScheduler(votationRepository, transitionService);
  }

  @Test
  void reschedulesActivityToMostVotedOptionWhenParticipationReachesQuorum() {
    Activity activity = activity(3);
    LocalDateTime winner = LocalDateTime.of(2026, 9, 3, 10, 0);
    Votation votation =
        votation(
            option(winner, activity.getParticipants().get(0), activity.getParticipants().get(1)),
            option(winner.plusHours(1), activity.getParticipants().get(2)));
    when(votationRepository.findActiveDueToClose(any())).thenReturn(List.of(votation));
    when(activityRepository.findById("activity-1")).thenReturn(activity);
    votation.setActivity(activity);

    scheduler.closeDueVotations();

    assertThat(votation.getStatus()).isEqualTo(VotationStatus.CLOSED);
    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.RESCHEDULED);
    assertThat(activity.getDateTime()).isEqualTo(winner);
    verify(votationRepository).save(votation);
    verify(activityRepository).save(activity);
    assertNotificationType(ReprogrammedNotificationType.class);
  }

  @Test
  void cancelsActivityWhenTotalParticipationDoesNotReachQuorum() {
    Activity activity = activity(3);
    Votation votation =
        votation(
            option(LocalDateTime.of(2026, 9, 3, 10, 0), activity.getParticipants().getFirst()));
    when(votationRepository.findActiveDueToClose(any())).thenReturn(List.of(votation));
    when(activityRepository.findById("activity-1")).thenReturn(activity);
    votation.setActivity(activity);

    scheduler.closeDueVotations();

    assertThat(votation.getStatus()).isEqualTo(VotationStatus.CLOSED);
    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
    assertNotificationType(CancelledNotificationType.class);
  }

  @Test
  void persistsResolutionBeforeNotificationFailure() {
    Activity activity = activity(1);
    Votation votation =
        votation(
            option(LocalDateTime.of(2026, 9, 3, 10, 0), activity.getParticipants().getFirst()));
    when(votationRepository.findActiveDueToClose(any())).thenReturn(List.of(votation));
    when(activityRepository.findById("activity-1")).thenReturn(activity);
    votation.setActivity(activity);
    doThrow(new RuntimeException("notification failed"))
        .when(eventPublisher)
        .publishEvent(any(ActivityNotificationEvent.class));

    org.assertj.core.api.Assertions.assertThatThrownBy(scheduler::closeDueVotations)
        .isInstanceOf(RuntimeException.class);

    verify(votationRepository).save(votation);
    verify(activityRepository).save(activity);
  }

  @Test
  void ignoresOrphanVotationWithoutClosingIt() {
    Votation votation = votation();
    when(votationRepository.findActiveDueToClose(any())).thenReturn(List.of(votation));
    when(activityRepository.findById("activity-1")).thenReturn(null);

    scheduler.closeDueVotations();

    assertThat(votation.getStatus()).isEqualTo(VotationStatus.ACTIVE);
    verify(votationRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  private Activity activity(int participantCount) {
    Activity activity = new Activity();
    activity.setId("activity-1");
    activity.setOrganizer(namedUser("organizer"));
    activity.setParticipants(
        java.util.stream.IntStream.range(0, participantCount)
            .mapToObj(index -> namedUser("participant-" + index))
            .toList());
    activity.setDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));
    activity.setStatus(ActivityStatus.PROPOSED);
    return activity;
  }

  private User namedUser(String id) {
    User user = User.withId(id);
    user.setName(id);
    return user;
  }

  private Votation votation(VotationOption... options) {
    Votation votation = new Votation();
    votation.setId("votation-1");
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setMinQuorum(0.5);
    votation.setOptions(List.of(options));
    return votation;
  }

  private VotationOption option(LocalDateTime date, User... users) {
    VotationOption option = new VotationOption();
    option.setDateTime(date);
    option.setUsers(List.of(users));
    return option;
  }

  private void assertNotificationType(Class<?> expectedType) {
    ArgumentCaptor<ActivityNotificationEvent> captor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().type()).isInstanceOf(expectedType);
  }
}
