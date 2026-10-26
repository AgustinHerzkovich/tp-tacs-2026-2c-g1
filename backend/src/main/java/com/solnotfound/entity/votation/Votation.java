package com.solnotfound.entity.votation;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "votations")
@CompoundIndex(name = "active_closing_date", def = "{'status': 1, 'closingDate': 1}")
public class Votation {

  @Id @Getter @Setter private String id;

  @DocumentReference(lazy = true)
  private Activity activity;

  @Getter @Setter private LocalDateTime creationDate;
  private LocalDateTime closingDate;

  /**
   * Minimum participation (0..1) required to resolve this votation. There is no default on the
   * entity: every creation path sets it explicitly from the {@code votation.min-quorum} property,
   * and {@code null} means "no quorum configured", which {@link #reachesQuorum(int)} refuses.
   */
  private Double minQuorum;

  @Getter @Setter private VotationStatus status;

  /**
   * Number of times this votation was stored. The repository only accepts a save made over the
   * version it read, so a vote, an organizer edit and the closing pass cannot overwrite each other.
   * Votations stored before this field existed read as version 0.
   */
  private long version;

  private List<VotationOption> options = new ArrayList<>();

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "The votation intentionally references its activity aggregate")
  public Activity getActivity() {
    return activity;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "The votation intentionally references its activity aggregate")
  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public synchronized List<VotationOption> getOptions() {
    return List.copyOf(options);
  }

  public synchronized void setOptions(List<VotationOption> options) {
    this.options = new ArrayList<>(options);
  }

  public synchronized LocalDateTime getClosingDate() {
    return closingDate;
  }

  public synchronized void setClosingDate(LocalDateTime closingDate) {
    this.closingDate = closingDate;
  }

  public synchronized long getVersion() {
    return version;
  }

  public synchronized void setVersion(long version) {
    this.version = version;
  }

  public synchronized Double getMinQuorum() {
    return minQuorum;
  }

  public synchronized void setMinQuorum(Double minQuorum) {
    this.minQuorum = minQuorum;
  }

  public synchronized boolean isAnOption(LocalDateTime option) {
    return options.stream().anyMatch((votOpt) -> votOpt.getDateTime().equals(option));
  }

  public synchronized boolean thisUserVoted(User user) {
    return options.stream().anyMatch((option) -> option.thisUserVoted(user));
  }

  private synchronized Optional<VotationOption> findOption(LocalDateTime option) {
    return options.stream().filter(o -> o.getDateTime().equals(option)).findFirst();
  }

  public synchronized void vote(LocalDateTime option, User user) {
    final Optional<VotationOption> optionFound = findOption(option);
    if (optionFound.isPresent()) {
      optionFound.get().addUser(user);
    }
  }

  public synchronized void unvote(LocalDateTime option, User user) {
    final Optional<VotationOption> optionFound = findOption(option);
    if (optionFound.isPresent()) {
      optionFound.get().removeUser(user);
    }
  }

  public synchronized Optional<LocalDateTime> getVoteByUser(User user) {
    for (VotationOption option : options) {
      if (option.thisUserVoted(user)) {
        return Optional.of(option.getDateTime());
      }
    }
    return Optional.empty();
  }

  /**
   * Finds the option chosen by a user, comparing only user identifiers.
   *
   * <p>Unlike {@link #getVoteByUser(User)}, this does not rely on {@code User.equals}, which also
   * compares display names; a user reference built from an identifier alone therefore still matches
   * a stored voter.
   *
   * @param userId identifier of the user whose vote is requested
   * @return date and time of the chosen option, or empty when the user has not voted
   */
  public synchronized Optional<LocalDateTime> getVoteByUserId(String userId) {
    for (VotationOption option : options) {
      if (option.getUsers().stream().anyMatch(user -> user.getId().equals(userId))) {
        return Optional.of(option.getDateTime());
      }
    }
    return Optional.empty();
  }

  /**
   * Indicates whether this active votation reached its configured closing instant.
   *
   * @param now instant used for the comparison
   * @return {@code true} when the votation is active and its closing date is not after {@code now}
   */
  public synchronized boolean isDueToClose(LocalDateTime now) {
    return status == VotationStatus.ACTIVE && closingDate != null && !closingDate.isAfter(now);
  }

  /**
   * Determines whether total participation reaches the configured quorum. A user is counted once
   * across the whole votation, independently of the selected option.
   *
   * @param eligibleVoters organizer and participants allowed to vote
   * @return {@code true} when the number of distinct voters reaches the rounded-up quorum
   */
  public synchronized boolean reachesQuorum(int eligibleVoters) {
    if (eligibleVoters <= 0 || minQuorum == null || minQuorum < 0 || minQuorum > 1) {
      return false;
    }
    Set<String> voterIds = new HashSet<>();
    for (VotationOption option : options) {
      option.getUsers().stream().map(User::getId).forEach(voterIds::add);
    }
    return voterIds.size() >= Math.ceil(eligibleVoters * minQuorum);
  }

  /**
   * Selects the option with the most votes. Ties are resolved deterministically in favor of the
   * earliest date.
   *
   * @return the winning date, or empty when there are no options
   */
  public synchronized Optional<LocalDateTime> winningOption() {
    return winningOption(LocalDateTime.MIN);
  }

  /**
   * Selects the most voted option among those that are still in the future. An alternative whose
   * date already passed cannot be chosen: rescheduling to it would move the activity to the past.
   * Ties are resolved in favor of the earliest date.
   *
   * @param now current time in the activity's zone
   * @return the winning date, or empty when no option is after {@code now}
   */
  public synchronized Optional<LocalDateTime> winningOption(LocalDateTime now) {
    return options.stream()
        .filter(option -> option.getDateTime().isAfter(now))
        .max(
            Comparator.comparingInt((VotationOption option) -> option.getUsers().size())
                .thenComparing(VotationOption::getDateTime, Comparator.reverseOrder()))
        .map(VotationOption::getDateTime);
  }
}
