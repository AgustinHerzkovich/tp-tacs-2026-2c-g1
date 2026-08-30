package com.solnotfound.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VotationOptionDTO(LocalDateTime dateTime, List<UserDTO> users) {

  public VotationOptionDTO {
    users = List.copyOf(users);
  }

  @Override
  public List<UserDTO> users() {
    return List.copyOf(users);
  }
}
