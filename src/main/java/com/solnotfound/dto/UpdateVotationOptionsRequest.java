package com.solnotfound.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateVotationOptionsRequest(@NotEmpty List<@NotNull LocalDateTime> dates) {

  public UpdateVotationOptionsRequest {
    dates = List.copyOf(dates);
  }

  @Override
  public List<LocalDateTime> dates() {
    return List.copyOf(dates);
  }
}
