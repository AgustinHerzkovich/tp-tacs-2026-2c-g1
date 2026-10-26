package com.solnotfound.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Body of {@code PUT /votations/{id}/options}: the votation's alternative dates are replaced by
 * {@code dates}.
 *
 * @param dates the alternative dates the votation should end up with
 * @param allowVoteLoss optional acknowledgement required to drop alternatives that already carry
 *     votes; when absent or {@code false} the request is rejected instead of silently discarding
 *     those votes. Clients that know which alternatives have votes (the response exposes each
 *     option's {@code voteCount}) should ask the organizer and only then retry with {@code true}.
 */
public record UpdateVotationOptionsRequest(
    @NotEmpty List<@NotNull LocalDateTime> dates, Boolean allowVoteLoss) {

  public UpdateVotationOptionsRequest {
    dates = List.copyOf(dates);
  }

  public UpdateVotationOptionsRequest(List<LocalDateTime> dates) {
    this(dates, null);
  }

  @Override
  public List<LocalDateTime> dates() {
    return List.copyOf(dates);
  }
}
