package com.solnotfound.repository;

import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class VotationRepository implements IVotationRepository {

  private final MongoVotationRepository repository;
  private final MongoTemplate mongoTemplate;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared MongoTemplate bean")
  public VotationRepository(MongoVotationRepository repository, MongoTemplate mongoTemplate) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
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
    long readVersion = votation.getVersion();
    Criteria unchanged =
        readVersion == 0
            ? new Criteria()
                .orOperator(
                    Criteria.where("version").is(0L), Criteria.where("version").exists(false))
            : Criteria.where("version").is(readVersion);
    Query storedAsRead =
        Query.query(
            new Criteria().andOperator(Criteria.where("_id").is(votation.getId()), unchanged));
    votation.setVersion(readVersion + 1);
    if (mongoTemplate.findAndReplace(storedAsRead, votation) != null) {
      return votation;
    }
    if (!repository.existsById(votation.getId())) {
      return mongoTemplate.insert(votation);
    }
    votation.setVersion(readVersion);
    throw new OptimisticLockingFailureException(
        "Votation changed since it was read: " + votation.getId());
  }

  @Override
  public void removeVotes(String activityId, String userId) {
    Query votedActive =
        Query.query(
            Criteria.where("activity")
                .is(activityId)
                .and("status")
                .is(VotationStatus.ACTIVE)
                .and("options.users")
                .is(userId));

    mongoTemplate.updateMulti(
        votedActive,
        new Update().pull("options.$[].users", userId).inc("version", 1),
        Votation.class);
  }

  @Override
  public List<Votation> findByActivityIds(List<String> activityIds) {
    return repository.findByActivityIds(activityIds);
  }

  @Override
  public Page<Votation> search(
      List<String> activityIds, VotationFilterDTO filter, String userId, Pageable pageable) {
    Pageable newestFirst =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "creationDate"));
    if (activityIds.isEmpty()) {
      return Page.empty(newestFirst);
    }
    // Activities and voters are stored as document references, i.e. as plain identifiers.
    Query query = Query.query(Criteria.where("activity").in(activityIds));
    if (filter.status() != null) {
      query.addCriteria(Criteria.where("status").is(filter.status()));
    }
    if (filter.votedByMe() != null) {
      query.addCriteria(
          filter.votedByMe()
              ? Criteria.where("options.users").is(userId)
              : Criteria.where("options.users").ne(userId));
    }
    long total = mongoTemplate.count(query, Votation.class);
    List<Votation> content = mongoTemplate.find(query.with(newestFirst), Votation.class);
    return new PageImpl<>(content, newestFirst, total);
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
