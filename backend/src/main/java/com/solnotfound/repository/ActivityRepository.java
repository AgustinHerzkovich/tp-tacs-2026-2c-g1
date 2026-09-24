package com.solnotfound.repository;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository implements IActivityRepository {
  private final MongoActivityRepository repository;
  private final MongoTemplate mongoTemplate;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared MongoTemplate bean")
  public ActivityRepository(MongoActivityRepository repository, MongoTemplate mongoTemplate) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void save(Activity activity) {
    repository.save(activity);
  }

  @Override
  public List<Activity> findAll() {
    return repository.findAll();
  }

  @Override
  public List<Activity> findActive() {
    return repository.findByStatusNotIn(List.of(ActivityStatus.CANCELLED, ActivityStatus.FINISHED));
  }

  @Override
  public Activity findById(String id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public void deleteAll() {
    repository.deleteAll();
  }

  @Override
  public List<Activity> findActivitiesByOrganizerId(String organizerId) {
    return repository.findByOrganizerId(organizerId);
  }

  @Override
  public List<Activity> findActivitiesByParticipantId(String participantId) {
    return repository.findByParticipantId(participantId);
  }

  /** Searches and paginates activities in MongoDB, applying all supplied filters before paging. */
  @Override
  public Page<Activity> search(ActivityFilterDTO filter, Pageable pageable) {
    Query query = new Query();
    if (filter.type() != null) {
      query.addCriteria(Criteria.where("type").is(filter.type()));
    }
    if (!filter.statuses().isEmpty()) {
      query.addCriteria(Criteria.where("status").in(filter.statuses()));
    }
    if (filter.city() != null && !filter.city().isBlank()) {
      query.addCriteria(
          Criteria.where("location.city.name").regex(Pattern.quote(filter.city().trim()), "i"));
    }
    if (filter.title() != null && !filter.title().isBlank()) {
      query.addCriteria(Criteria.where("title").regex(Pattern.quote(filter.title().trim()), "i"));
    }
    if (!filter.ids().isEmpty()) {
      query.addCriteria(Criteria.where("_id").in(filter.ids()));
    }
    if (filter.availability() != null) {
      query.addCriteria(availabilityCriteria(filter.availability()));
    }
    if (filter.dateFrom() != null || filter.dateTo() != null) {
      Criteria dates = Criteria.where("dateTime");
      if (filter.dateFrom() != null) {
        dates = dates.gte(filter.dateFrom());
      }
      if (filter.dateTo() != null) {
        dates = dates.lte(filter.dateTo());
      }
      query.addCriteria(dates);
    }
    return page(query, pageable);
  }

  private Criteria availabilityCriteria(boolean available) {
    MongoExpression hasCapacity =
        MongoExpression.create(
            "{ $lt: [ { $size: { $ifNull: [ '$participants', [] ] } }, '$maxParticipants' ] }");
    Criteria changeableStatus =
        Criteria.where("status").nin(ActivityStatus.CANCELLED, ActivityStatus.FINISHED);

    if (available) {
      return new Criteria().andOperator(changeableStatus, Criteria.expr(hasCapacity));
    }
    return new Criteria()
        .orOperator(
            Criteria.where("status").in(ActivityStatus.CANCELLED, ActivityStatus.FINISHED),
            Criteria.expr(
                MongoExpression.create(
                    "{ $not: [ { $lt: [ { $size: { $ifNull: [ '$participants', [] ] } }, '$maxParticipants' ] } ] }")));
  }

  @Override
  public Page<Activity> findActivitiesByOrganizerId(String organizerId, Pageable pageable) {
    return page(Query.query(Criteria.where("organizer").is(organizerId)), pageable);
  }

  @Override
  public Page<Activity> findActivitiesByParticipantId(String participantId, Pageable pageable) {
    return page(Query.query(Criteria.where("participants").is(participantId)), pageable);
  }

  private Page<Activity> page(Query query, Pageable pageable) {
    long total = mongoTemplate.count(query, Activity.class);
    List<Activity> content = mongoTemplate.find(query.with(pageable), Activity.class);
    return new PageImpl<>(content, pageable, total);
  }
}
