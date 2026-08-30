package com.solnotfound.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Votation {

  private String id;
  private Activity activity;
  private LocalDateTime creationDate;
  private LocalDateTime closeDate;
  private Double minQuorum = 0.50; // 50%
  private VotationStatus status;
  private List<VotationOption> options = new ArrayList<>();

  public List<VotationOption> getOptions() {
    return List.copyOf(options);
  }

  public void setOptions(List<VotationOption> options) {
    this.options = new ArrayList<>(options);
  }

  public boolean isAnOption(LocalDateTime option) {
    return options.stream().anyMatch((votOpt) -> votOpt.getDateTime().equals(option));
  }

  public boolean thisUserVoted(User user) {
    return options.stream().anyMatch((option) -> option.thisUserVoted(user));
  }

  public void vote(LocalDateTime option, User user) {
    options.get(options.indexOf(option)).addUser(user);
  }

  public void unvote(LocalDateTime option, User user) {
    options.get(options.indexOf(option)).removeUser(user);
  }

  public LocalDateTime getVoteByUser(User user) {
    for (VotationOption option : options) {
      if (option.thisUserVoted(user)) {
        return option.getDateTime();
      }
    }
  }
}
