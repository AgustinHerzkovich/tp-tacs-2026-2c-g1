package com.solnotfound.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VotationResolutionTest {

  @Test
  void quorumCountsDistinctParticipationAcrossAllOptions() {
    User first = User.withId("first");
    User second = User.withId("second");
    Votation votation = votation(option(10, first), option(11, second));
    votation.setMinQuorum(0.5);

    assertThat(votation.reachesQuorum(4)).isTrue();
    assertThat(votation.reachesQuorum(5)).isFalse();
  }

  @Test
  void quorumRoundsRequiredParticipationUp() {
    Votation votation = votation(option(10, User.withId("first")));
    votation.setMinQuorum(0.5);

    assertThat(votation.reachesQuorum(3)).isFalse();
  }

  @Test
  void selectsMostVotedOptionAndUsesEarliestDateForTie() {
    Votation mostVoted =
        votation(
            option(10, User.withId("first")),
            option(11, User.withId("second"), User.withId("third")));
    Votation tied = votation(option(10, User.withId("first")), option(11, User.withId("second")));

    assertThat(mostVoted.winningOption()).contains(dateAt(11));
    assertThat(tied.winningOption()).contains(dateAt(10));
  }

  @Test
  void onlyActiveVotationAtOrPastClosingDateIsDue() {
    LocalDateTime now = LocalDateTime.of(2026, 9, 1, 12, 0);
    Votation votation = votation();
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setClosingDate(now);

    assertThat(votation.isDueToClose(now)).isTrue();
    assertThat(votation.isDueToClose(now.minusNanos(1))).isFalse();
    votation.setStatus(VotationStatus.CLOSED);
    assertThat(votation.isDueToClose(now)).isFalse();
  }

  private Votation votation(VotationOption... options) {
    Votation votation = new Votation();
    votation.setOptions(List.of(options));
    return votation;
  }

  private VotationOption option(int hour, User... users) {
    VotationOption option = new VotationOption();
    option.setDateTime(dateAt(hour));
    option.setUsers(List.of(users));
    return option;
  }

  private LocalDateTime dateAt(int hour) {
    return LocalDateTime.of(2026, 9, 2, hour, 0);
  }
}
