package com.solnotfound.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VotationOptionDTO(LocalDateTime dateTime, int voteCount, List<String> voterNames) {

  public VotationOptionDTO {
    voterNames = List.copyOf(voterNames);
  }

  @Override
  public List<String> voterNames() {
    return List.copyOf(voterNames);
  }
}
