package com.solnotfound.dto;

import com.solnotfound.entity.VotationStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

public record VotationDTO(
    String id,
    ActivityDTO activity,
    LocalDateTime creationDate,
    VotationStatus status,
    List<VotationOptionDTO> options) {

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public VotationDTO {}

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<VotationOptionDTO> options() {
    return options;
  }
}
