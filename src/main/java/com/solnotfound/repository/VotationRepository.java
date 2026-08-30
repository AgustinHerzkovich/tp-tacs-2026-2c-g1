package com.solnotfound.repository;

import com.solnotfound.entity.Votation;
import java.util.List;

public interface VotationRepository {
  public Votation findById(String id);

  public List<Votation> findAll();

  public void save(Votation entity);

  public void update(Votation entity);

  public List<Votation> findByVoterId(String id);

  public List<Votation> findByOrganizerOrParticipantId(String userId);

  public Votation findActiveVotationByActivityId(String activityId);
}
