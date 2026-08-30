package com.solnotfound.repository;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class VotationMockRepository implements VotationRepository {

  private final List<Votation> votations = new ArrayList<>();

  @Override
  public Votation findById(String id) {
    return votations.stream()
        .filter(votation -> Objects.equals(votation.getId(), id))
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<Votation> findAll() {
    return List.copyOf(votations);
  }

  @Override
  public void save(Votation entity) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
      votations.add(entity);
      return;
    }

    if (findById(entity.getId()) == null) {
      votations.add(entity);
    }
  }

  @Override
  public void update(Votation entity) {
    for (int i = 0; i < votations.size(); i++) {
      if (Objects.equals(votations.get(i).getId(), entity.getId())) {
        votations.set(i, entity);
        return;
      }
    }
    votations.add(entity);
  }

  @Override
  public List<Votation> findByVoterId(String voterId) {
    return votations.stream()
        .filter(
            votation ->
                hasParticipants(votation.getActivity())
                    && votation.getActivity().getParticipants().stream()
                        .anyMatch(participant -> voterId.equals(participant.getId())))
        .toList();
  }

  @Override
  public List<Votation> findByOrganizerOrParticipantId(String userId) {
    return votations.stream()
        .filter(
            votation ->
                isOrganizer(votation.getActivity(), userId)
                    || isParticipant(votation.getActivity(), userId))
        .toList();
  }

  @Override
  public Votation findActiveVotationByActivityId(String activityId) {
    return votations.stream()
        .filter(votation -> votation.getStatus() == VotationStatus.ACTIVE)
        .filter(
            votation ->
                votation.getActivity() != null && activityId.equals(votation.getActivity().getId()))
        .findFirst()
        .orElse(null);
  }

  private boolean isOrganizer(Activity activity, String userId) {
    return activity != null && matchesUser(activity.getOrganizer(), userId);
  }

  private boolean isParticipant(Activity activity, String userId) {
    return hasParticipants(activity)
        && activity.getParticipants().stream()
            .anyMatch(participant -> matchesUser(participant, userId));
  }

  private boolean hasParticipants(Activity activity) {
    return activity != null && activity.getParticipants() != null;
  }

  private boolean matchesUser(User user, String userId) {
    return user != null && userId.equals(user.getId());
  }
}
