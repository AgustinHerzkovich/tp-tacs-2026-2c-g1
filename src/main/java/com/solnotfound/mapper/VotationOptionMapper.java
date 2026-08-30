package com.solnotfound.mapper;

import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.entity.User;
import com.solnotfound.entity.VotationOption;

public final class VotationOptionMapper {

  private VotationOptionMapper() {}

  public static VotationOptionDTO toDTO(VotationOption option) {
    if (option == null) {
      return null;
    }

    return new VotationOptionDTO(
        option.getDateTime(),
        option.getUsers().size(),
        option.getUsers().stream().map(VotationOptionMapper::displayName).toList());
  }

  private static String displayName(User user) {
    return user.getName() == null || user.getName().isBlank() ? user.getId() : user.getName();
  }
}
