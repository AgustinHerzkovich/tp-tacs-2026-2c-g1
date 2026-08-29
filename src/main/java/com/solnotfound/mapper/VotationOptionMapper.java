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

  public static VotationOption toEntity(VotationOptionDTO optionDTO) {
    if (optionDTO == null) {
      return null;
    }

    VotationOption option = new VotationOption();
    option.setDateTime(optionDTO.dateTime());
    option.setUsers(toUsers(optionDTO));

    return option;
  }

  private static List<com.solnotfound.dto.UserDTO> toUserDTOs(VotationOption option) {
    if (option.getUsers() == null) {
      return null;
    }

    return option.getUsers().stream().map(UserMapper::toDTO).toList();
  }

  private static List<com.solnotfound.entity.User> toUsers(VotationOptionDTO optionDTO) {
    if (optionDTO.users() == null) {
      return null;
    }

    return optionDTO.users().stream().map(UserMapper::toEntity).toList();
  }
}
