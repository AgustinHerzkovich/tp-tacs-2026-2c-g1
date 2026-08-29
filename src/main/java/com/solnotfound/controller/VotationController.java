package com.solnotfound.controller;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.service.VotationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/votations")
public class VotationController {

  private final VotationService votationService;

  public VotationController(VotationService votationService) {
    this.votationService = votationService;
  }

  @GetMapping("/users/{userId}")
  public ResponseEntity<List<VotationDTO>> getByOrganizerOrParticipantId(
      @PathVariable String userId) {
    List<VotationDTO> votations = votationService.getByOrganizerOrParticipantId(userId);
    if (votations == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(votations);
  }
}
