package com.solnotfound.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.NotificationService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevExtrasSeederTest {

  @Mock private IActivityRepository activityRepository;
  @Mock private IUserRepository userRepository;
  @Mock private IVotationRepository votationRepository;
  @Mock private NotificationService notificationService;

  private Activity activity;
  private DevExtrasSeeder seeder;

  @BeforeEach
  void setUp() {
    activity = new Activity();
    activity.setId("activity-1");
    activity.setTitle("Actividad");
    activity.setOrganizer(User.withId("user-1"));
    activity.setDateTime(LocalDateTime.of(2026, 9, 20, 15, 0));
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
    when(activityRepository.findActivitiesByOrganizerId("user-1")).thenReturn(List.of(activity));

    seeder =
        new DevExtrasSeeder(
            activityRepository, userRepository, votationRepository, notificationService, "user-1");
  }

  @Test
  void createsThreeDistinctOptionsInsideTheConfiguredRange() {
    seeder.run();

    ArgumentCaptor<Votation> captor = ArgumentCaptor.forClass(Votation.class);
    verify(votationRepository).save(captor.capture());
    Votation votation = captor.getValue();

    assertEquals(3, votation.getOptions().size());
    assertEquals(
        3, votation.getOptions().stream().map(option -> option.getDateTime()).distinct().count());
    votation
        .getOptions()
        .forEach(
            option ->
                assertTrue(
                    activity
                        .getReprogramationRange()
                        .isWithinRange(activity.getDateTime(), option.getDateTime())));
    assertEquals(ActivityStatus.PROPOSED, activity.getStatus());
    verify(notificationService, org.mockito.Mockito.times(3))
        .generateNotificationsForActivityEvent(any(), any());
  }

  @Test
  void doesNotOverwriteAnExistingSeededVotationOrDuplicateNotifications() {
    Votation existing = new Votation();
    existing.setId("dev-votation-activity-1");
    existing.setStatus(VotationStatus.CLOSED);
    when(votationRepository.findById("dev-votation-activity-1")).thenReturn(existing);

    seeder.run();

    verify(votationRepository, never()).save(any());
    verify(notificationService, never()).generateNotificationsForActivityEvent(any(), any());
  }
}
