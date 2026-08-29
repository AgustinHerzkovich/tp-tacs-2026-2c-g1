package com.solnotfound.service;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Votation;
import com.solnotfound.mapper.VotationMapper;
import com.solnotfound.repository.VotationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VotationService {

  private final VotationRepository votationRepository;

  public VotationService(VotationRepository votationRepository) {
    this.votationRepository = votationRepository;
  }

  public List<VotationDTO> getByOrganizerOrParticipantId(String userId) {
    List<Votation> votations = votationRepository.findByOrganizerOrParticipantId(userId);
    if (votations == null) {
      return null;
    }
    return votations.stream().map(VotationMapper::toDTO).toList();
  }
}
