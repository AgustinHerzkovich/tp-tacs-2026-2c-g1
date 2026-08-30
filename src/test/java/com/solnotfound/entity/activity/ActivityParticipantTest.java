package com.solnotfound.entity.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.Participant;
import com.solnotfound.exception.IllegalStateActivityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActivityParticipantTest {

  private Activity activity;

  @BeforeEach
  void setUp() {
    activity = new Activity();
    activity.setMinParticipants(2);
    activity.setMaxParticipants(3);
  }

  @Test
  void newActivityStartsConfirmed() {
    Activity newActivity = new Activity();
    assertThat(newActivity.getStatus()).isEqualTo(ActivityStatus.CONFIRMED);
  }

  @Test
  void addsParticipant() {
    activity.addParticipant("user-1");

    assertThat(activity.getParticipants())
        .extracting(Participant::getUserId)
        .containsExactly("user-1");
  }

  @Test
  void doesNotMakeActivityAvailableBeforeMinimumParticipants() {
    activity.addParticipant("user-1");

    assertThat(activity.getAvailability()).isFalse();
  }

  @Test
  void makesActivityAvailableWhenMinimumParticipantsIsReached() {
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");

    assertThat(activity.getAvailability()).isTrue();
  }

  @Test
  void doesNotAllowSameUserToJoinTwice() {
    activity.addParticipant("user-1");
    activity.addParticipant("user-1");

    assertThat(activity.getParticipants())
        .extracting(Participant::getUserId)
        .containsExactly("user-1");
  }

  @Test
  void doesNotAllowMoreParticipantsThanMaximum() {
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");
    activity.addParticipant("user-3");

    assertThatThrownBy(() -> activity.addParticipant("user-4"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Activity has no available spots.");
  }

  @Test
  void removesParticipant() {
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");

    activity.removeParticipant("user-1");

    assertThat(activity.getParticipants())
        .extracting(Participant::getUserId)
        .containsExactly("user-2");
  }

  @Test
  void doesNotAllowRemovingUserWhoIsNotParticipating() {
    activity.removeParticipant("user-1");

    assertThat(activity.getParticipants()).isEmpty();
  }

  @Test
  void makesActivityUnavailableWhenParticipantsDropBelowMinimum() {
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");

    assertThat(activity.getAvailability()).isTrue();

    activity.removeParticipant("user-1");

    assertThat(activity.getAvailability()).isFalse();
  }

  @Test
  void allowsRemovingParticipantWhenActivityStillHasMinimumParticipants() {
    /*This test verifies that when a participant is removed, the activity
    remains available if it still meets the minimum number of participants.*/
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");
    activity.addParticipant("user-3");

    activity.removeParticipant("user-1");

    assertThat(activity.getAvailability()).isTrue();
    assertThat(activity.getParticipants()).hasSize(2);
  }

  @Test
  void doesNotAllowAddingParticipantToCancelledActivity() {
    activity.setStatus(ActivityStatus.CANCELLED);

    assertThatThrownBy(() -> activity.addParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Participants cannot be added to an activity in that status.");
  }

  @Test
  void doesNotAllowAddingParticipantToFinishedActivity() {
    activity.setStatus(ActivityStatus.FINISHED);

    assertThatThrownBy(() -> activity.addParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Participants cannot be added to an activity in that status.");
  }

  @Test
  void doesNotAllowRemovingParticipantFromCancelledActivity() {
    activity.addParticipant("user-1");
    activity.setStatus(ActivityStatus.CANCELLED);

    assertThatThrownBy(() -> activity.removeParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Participants cannot be removed to an activity in that status.");
  }

  @Test
  void doesNotAllowRemovingParticipantFromFinishedActivity() {
    activity.addParticipant("user-1");
    activity.setStatus(ActivityStatus.FINISHED);

    assertThatThrownBy(() -> activity.removeParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Participants cannot be removed to an activity in that status.");
  }

  @Test
  void newActivityStartsWithConfirmedStatusInHistory() {
    Activity activity = new Activity();

    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CONFIRMED);
    assertThat(activity.getStatusHistory()).containsExactly(ActivityStatus.CONFIRMED);
  }

  @Test
  void changingStatusAddsItToHistory() {
    Activity activity = new Activity();

    activity.setStatus(ActivityStatus.PROPOSED);

    assertThat(activity.getStatusHistory())
        .containsExactly(ActivityStatus.CONFIRMED, ActivityStatus.PROPOSED);
  }

  @Test
  void concurrentJoinsDoNotExceedMaximumParticipants() throws Exception {
    activity.setMaxParticipants(1);
    CountDownLatch start = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> joinAfter(start, "user-1"));
      executor.submit(() -> joinAfter(start, "user-2"));
      start.countDown();
    }

    assertThat(activity.getParticipants()).hasSize(1);
  }

  @Test
  void concurrentDuplicateJoinsAddUserOnce() throws Exception {
    CountDownLatch start = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> joinAfter(start, "user-1"));
      executor.submit(() -> joinAfter(start, "user-1"));
      start.countDown();
    }

    assertThat(activity.getParticipants())
        .extracting(Participant::getUserId)
        .containsExactly("user-1");
  }

  private void joinAfter(CountDownLatch start, String userId) {
    try {
      start.await();
      activity.addParticipant(userId);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    } catch (IllegalStateActivityException ignored) {
      // One contender is expected to lose when the activity reaches capacity.
    }
  }
}
