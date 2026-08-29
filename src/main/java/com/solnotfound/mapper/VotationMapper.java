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
        ActivityMapper.toDTO(votation.getActivity()),
        votation.getCreationDate(),
        votation.getStatus(),
        toOptionDTOs(votation));
  }

  public static Votation toEntity(VotationDTO votationDTO) {
    if (votationDTO == null) {
      return null;
    }

    Votation votation = new Votation();
    votation.setId(votationDTO.id());
    votation.setActivity(ActivityMapper.toEntity(votationDTO.activity()));
    votation.setCreationDate(votationDTO.creationDate());
    votation.setStatus(votationDTO.status());
    votation.setOptions(toOptions(votationDTO));

    return votation;
  }

  private static List<com.solnotfound.dto.VotationOptionDTO> toOptionDTOs(Votation votation) {
    if (votation.getOptions() == null) {
      return null;
    }

    return votation.getOptions().stream().map(VotationOptionMapper::toDTO).toList();
  }

  private static List<com.solnotfound.entity.VotationOption> toOptions(VotationDTO votationDTO) {
    if (votationDTO.options() == null) {
      return null;
    }

    return votationDTO.options().stream().map(VotationOptionMapper::toEntity).toList();
  }
}
