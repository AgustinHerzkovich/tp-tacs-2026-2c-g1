package com.solnotfound.mapper;

import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.entity.VotationOption;
import java.util.List;

public final class VotationOptionMapper {

  private VotationOptionMapper() {}

  public static VotationOptionDTO toDTO(VotationOption option) {
    if (option == null) {
      return null;
    }

    return new VotationOptionDTO(option.getDateTime(), toUserDTOs(option));
  }

  private static List<com.solnotfound.dto.UserDTO> toUserDTOs(VotationOption option) {
    if (option.getUsers() == null) {
      return null;
    }

    return option.getUsers().stream().map(UserMapper::toDTO).toList();
  }
}
