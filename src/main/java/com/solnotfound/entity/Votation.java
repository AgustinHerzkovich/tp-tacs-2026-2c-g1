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
  private String activityId;
  private LocalDateTime creationDate;
  private VotationStatus status;
  private List<VotationOption> options = new ArrayList<>();

  public List<VotationOption> getOptions() {
    return List.copyOf(options);
  }

  public void setOptions(List<VotationOption> options) {
    this.options = new ArrayList<>(options);
  }
}
