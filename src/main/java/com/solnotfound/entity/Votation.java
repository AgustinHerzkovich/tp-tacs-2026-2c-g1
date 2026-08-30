package com.solnotfound.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Votation {

  private String id;
  private String activityId;
  private LocalDateTime creationDate;
  private Double minQuorum = 0.50; // 50%
  private VotationStatus status;
  private List<VotationOption> options = new ArrayList<>();

  public synchronized List<VotationOption> getOptions() {
    return List.copyOf(options);
  }

  public synchronized void setOptions(List<VotationOption> options) {
    this.options = new ArrayList<>(options);
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
}
