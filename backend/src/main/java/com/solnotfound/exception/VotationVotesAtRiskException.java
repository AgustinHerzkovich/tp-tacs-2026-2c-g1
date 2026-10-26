package com.solnotfound.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Signals that replacing a votation's alternatives would silently discard votes already cast on the
 * options left out of the request.
 *
 * <p>It is a conflict rather than a plain invalid request because the very same request is valid
 * once the organizer confirms the loss: clients are expected to surface the affected dates and the
 * number of votes at stake, then retry with {@code allowVoteLoss} enabled.
 */
public class VotationVotesAtRiskException extends CodedException {

  private final List<LocalDateTime> optionDates;
  private final int votesAtRisk;

  public VotationVotesAtRiskException(List<LocalDateTime> optionDates, int votesAtRisk) {
    super(
        ErrorCode.VOTATION_VOTES_AT_RISK,
        "Replacing the options would discard "
            + votesAtRisk
            + " vote(s) already cast on: "
            + optionDates);
    this.optionDates = List.copyOf(optionDates);
    this.votesAtRisk = votesAtRisk;
  }

  /** Dates of the alternatives that would be removed while still carrying votes. */
  public List<LocalDateTime> getOptionDates() {
    return optionDates;
  }

  /** Total number of votes that would be lost across {@link #getOptionDates()}. */
  public int getVotesAtRisk() {
    return votesAtRisk;
  }
}
