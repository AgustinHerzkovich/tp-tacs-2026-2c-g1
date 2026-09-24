package com.solnotfound.repository;

import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.votation.Votation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    return repository.save(votation);
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
