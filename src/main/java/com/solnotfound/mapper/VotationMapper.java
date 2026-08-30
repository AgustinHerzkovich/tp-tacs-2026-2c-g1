package com.solnotfound.mapper;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Votation;
import java.util.List;

public final class VotationMapper {

  private VotationMapper() {}

  public static VotationDTO toDTO(Votation votation) {
    if (votation == null) {
      return null;
    }

    return new VotationDTO(
        votation.getId(),
        votation.getActivityId(),
        votation.getCreationDate(),
        votation.getStatus(),
        toOptionDTOs(votation));
  }

  private static List<com.solnotfound.dto.VotationOptionDTO> toOptionDTOs(Votation votation) {
    if (votation.getOptions() == null) {
      return null;
    }

    return votation.getOptions().stream().map(VotationOptionMapper::toDTO).toList();
  }
}
