package com.solnotfound.repository;

import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class InMemoryVotationRepository implements IVotationRepository {
  private final Map<String, Votation> votations = new ConcurrentHashMap<>();

  @Override
  public Votation findById(String id) {
    return votations.get(id);
  }

  @Override
  public List<Votation> findAll() {
    return List.copyOf(votations.values());
  }

  @Override
  public Votation save(Votation votation) {
    if (votation.getId() == null || votation.getId().isBlank()) {
      votation.setId(UUID.randomUUID().toString());
    }
    votations.put(votation.getId(), votation);
    return votation;
  }

  @Override
  public List<Votation> findByActivityIds(List<String> activityIds) {
    return votations.values().stream()
        .filter(votation -> activityIds.contains(votation.getActivity().getId()))
        .toList();
  }

  @Override
  public Page<Votation> search(
      List<String> activityIds, VotationFilterDTO filter, String userId, Pageable pageable) {
    List<Votation> matches =
        votations.values().stream()
            .filter(votation -> activityIds.contains(votation.getActivity().getId()))
            .filter(votation -> filter.status() == null || filter.status() == votation.getStatus())
            .filter(
                votation ->
                    filter.votedByMe() == null
                        || filter.votedByMe() == votation.getVoteByUserId(userId).isPresent())
            .sorted(
                Comparator.comparing(
                        Votation::getCreationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed())
            .toList();
    int from = (int) Math.min(pageable.getOffset(), matches.size());
    int to = Math.min(from + pageable.getPageSize(), matches.size());
    return new PageImpl<>(matches.subList(from, to), pageable, matches.size());
  }

  @Override
  public Votation findActiveByActivityId(String activityId) {
    return votations.values().stream()
        .filter(votation -> votation.getStatus() == VotationStatus.ACTIVE)
        .filter(votation -> activityId.equals(votation.getActivity().getId()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<Votation> findActiveDueToClose(LocalDateTime now) {
    return votations.values().stream().filter(votation -> votation.isDueToClose(now)).toList();
  }
}
