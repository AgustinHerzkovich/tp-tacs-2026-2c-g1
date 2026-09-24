package com.solnotfound.dto;

import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A votation as seen by one user.
 *
 * @param votedOption date and time of the option the requesting user voted for, or {@code null}
 *     when that user has not voted yet
 */
public record VotationDTO(
    String id,
    String activityId,
    LocalDateTime creationDate,
    VotationStatus status,
    List<VotationOptionDTO> options,
    LocalDateTime votedOption) {

  public VotationDTO {
    options = List.copyOf(options);
  }

  @Override
  public List<VotationOptionDTO> options() {
    return List.copyOf(options);
  }
}
