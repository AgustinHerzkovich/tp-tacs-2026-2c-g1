package com.solnotfound.mapper;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.votation.Votation;
import java.util.List;

public final class VotationMapper {

  private VotationMapper() {}

  public static VotationDTO toDTO(Votation votation, String currentUserId) {
    if (votation == null) {
      return null;
    }

    return new VotationDTO(
        votation.getId(),
        votation.getActivity().getId(),
        votation.getCreationDate(),
        votation.getStatus(),
        toOptionDTOs(votation),
        currentUserId == null ? null : votation.getVoteByUserId(currentUserId).orElse(null));
  }

  private static List<com.solnotfound.dto.VotationOptionDTO> toOptionDTOs(Votation votation) {
    if (votation.getOptions() == null) {
      return null;
    }

    return votation.getOptions().stream().map(VotationOptionMapper::toDTO).toList();
  }
}
