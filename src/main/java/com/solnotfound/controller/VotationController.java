package com.solnotfound.controller;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.service.VotationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/votations")
public class VotationController {

  private final VotationService votationService;

  public VotationController(VotationService votationService) {
    this.votationService = votationService;
  }

  @PutMapping("/{votationId}/options")
  public ResponseEntity<List<VotationDTO>> updateVotationOptions(
      @PathVariable String votationId, @Valid @RequestBody List<VotationOptionDTO> request) {
    List<VotationDTO> votations = votationService.updateVotationOptions(votationId, request);
    if (votations == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(votations);
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
