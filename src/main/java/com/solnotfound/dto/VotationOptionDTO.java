package com.solnotfound.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

public record VotationOptionDTO(LocalDateTime dateTime, List<UserDTO> users) {

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public VotationOptionDTO {}

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<UserDTO> users() {
    return users;
  }
}
