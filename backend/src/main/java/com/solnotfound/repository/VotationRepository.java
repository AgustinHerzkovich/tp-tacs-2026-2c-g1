package com.solnotfound.repository;

import com.solnotfound.entity.votation.Votation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class VotationRepository implements IVotationRepository {

  private final MongoVotationRepository repository;

  public VotationRepository(MongoVotationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Votation findById(String id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public List<Votation> findAll() {
    return repository.findAll();
  }

  @Override
  public Votation save(Votation votation) {
    if (votation.getId() == null || votation.getId().isBlank()) {
      votation.setId(UUID.randomUUID().toString());
    }
    return repository.save(votation);
  }

  @Override
  public List<Votation> findByActivityIds(List<String> activityIds) {
    return repository.findByActivityIds(activityIds);
  }

  @Override
  public Votation findActiveByActivityId(String activityId) {
    return repository.findActiveByActivityId(activityId).orElse(null);
  }

  @Override
  public List<Votation> findActiveDueToClose(LocalDateTime now) {
    return repository.findActiveDueToClose(now);
  }
}
