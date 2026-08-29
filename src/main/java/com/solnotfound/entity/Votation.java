package com.solnotfound.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Votation {

  private long id;
  private Activity activity;
  private LocalDateTime creationDate;
  private VotationStatus status;
  private List<VotationOption> options;

}
