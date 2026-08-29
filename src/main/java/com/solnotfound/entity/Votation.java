package com.solnotfound.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class Votation {

  private String id;
  private Activity activity;
  private LocalDateTime creationDate;
  private VotationStatus status;
  private List<VotationOption> options;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Activity getActivity() {
    return activity;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<VotationOption> getOptions() {
    return options;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOptions(List<VotationOption> options) {
    this.options = options;
  }
}
