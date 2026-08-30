package com.solnotfound.controller;

import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.service.VotationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/votations")
public class VotationController {

  private final VotationService votationService;

  public VotationController(VotationService votationService) {
    this.votationService = votationService;
  }

  @PutMapping("/{votationId}/options")
  public ResponseEntity<VotationDTO> updateVotationOptions(
      @PathVariable String votationId,
      @Valid @RequestBody UpdateVotationOptionsRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        votationService.updateVotationOptions(votationId, request, currentUserId(authentication)));
  }

  @GetMapping("/me")
  public ResponseEntity<List<VotationDTO>> getMine(Authentication authentication) {
    return ResponseEntity.ok(
        votationService.getByOrganizerOrParticipantId(currentUserId(authentication)));
  }

  private String currentUserId(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return "development-user";
  }
}
