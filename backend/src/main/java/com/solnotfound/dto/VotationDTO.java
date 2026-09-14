package com.solnotfound.dto;

import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record VotationDTO(
    String id,
    String activityId,
    LocalDateTime creationDate,
    VotationStatus status,
    List<VotationOptionDTO> options) {

  public VotationDTO {
    options = List.copyOf(options);
  }

  @Override
  public List<VotationOptionDTO> options() {
    return List.copyOf(options);
  }
}
