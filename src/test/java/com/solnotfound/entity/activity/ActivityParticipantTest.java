package com.solnotfound.entity.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.Participant;
import com.solnotfound.exception.IllegalStateActivityException;
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

    assertThatThrownBy(() -> activity.addParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("User is already participating in this activity.");
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
    assertThatThrownBy(() -> activity.removeParticipant("user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("User is not participating in this activity");
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
}
