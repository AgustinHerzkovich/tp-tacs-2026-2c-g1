package com.solnotfound.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VotationVoteTest {

  @Test
  void voteAssociatesUserWithChosenOption() {
    LocalDateTime chosen = dateAt(10);
    User user = User.withId("participant");
    Votation votation = votation(option(dateAt(10)), option(dateAt(11)));

    votation.vote(chosen, user);

    assertThat(votation.thisUserVoted(user)).isTrue();
    assertThat(votation.getVoteByUser(user)).contains(chosen);
    assertThat(votation.getOptions().get(0).thisUserVoted(user)).isTrue();
    assertThat(votation.getOptions().get(1).thisUserVoted(user)).isFalse();
  }

  @Test
  void userThatDidNotVoteIsNotMarkedAsVoter() {
    Votation votation = votation(option(dateAt(10)));

    assertThat(votation.thisUserVoted(User.withId("participant"))).isFalse();
    assertThat(votation.getVoteByUser(User.withId("participant"))).isEmpty();
  }

  @Test
  void votingSameOptionTwiceDoesNotDuplicateUser() {
    LocalDateTime chosen = dateAt(10);
    User user = User.withId("participant");
    Votation votation = votation(option(chosen));

    votation.vote(chosen, user);
    votation.vote(chosen, user);

    assertThat(votation.getOptions().getFirst().getUsers()).containsExactly(user);
  }

  @Test
  void unvoteRemovesUserFromOption() {
    LocalDateTime chosen = dateAt(10);
    User user = User.withId("participant");
    Votation votation = votation(option(chosen));
    votation.vote(chosen, user);

    votation.unvote(chosen, user);

    assertThat(votation.thisUserVoted(user)).isFalse();
    assertThat(votation.getOptions().getFirst().getUsers()).isEmpty();
  }

  private Votation votation(VotationOption... options) {
    Votation votation = new Votation();
    votation.setOptions(List.of(options));
    return votation;
  }

  private VotationOption option(LocalDateTime dateTime) {
    VotationOption option = new VotationOption();
    option.setDateTime(dateTime);
    return option;
  }

  private LocalDateTime dateAt(int hour) {
    return LocalDateTime.of(2026, 9, 2, hour, 0);
  }
}
