package com.solnotfound.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.user.User;
import com.solnotfound.observability.ActivitySearchTelemetry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository implements IActivityRepository {
  private final MongoActivityRepository repository;
  private final MongoTemplate mongoTemplate;
  private final ActivitySearchTelemetry searchTelemetry;

  /**
   * Totals of recent searches, by filter. Counting every match is what made the search saturate
   * MongoDB under load (docs/OBSERVABILIDAD.md), and a page only needs an approximate total to draw
   * its pagination. Every write to activities clears it; another instance may serve a total up to
   * the TTL old.
   */
  private final Cache<String, Long> searchTotals;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared MongoTemplate bean")
  @Autowired
  public ActivityRepository(
      MongoActivityRepository repository,
      MongoTemplate mongoTemplate,
      ActivitySearchTelemetry searchTelemetry,
      @Value("${activity.search.total-cache-ttl:30s}") Duration searchTotalTtl) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
    this.searchTelemetry = searchTelemetry;
    this.searchTotals =
        Caffeine.newBuilder().expireAfterWrite(searchTotalTtl).maximumSize(1_000).build();
  }

  public ActivityRepository(MongoActivityRepository repository, MongoTemplate mongoTemplate) {
    this(repository, mongoTemplate, ActivitySearchTelemetry.noop(), Duration.ofSeconds(30));
  }

  @Override
  public void save(Activity activity) {
    repository.save(activity);
    searchTotals.invalidateAll();
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
    searchTotals.invalidateAll();
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
    addDateRangeCriteria(query, filter.dateFrom(), filter.dateTo());
    return searchPage(query, pageable, filter.toString());
  }

  /**
   * Restricts {@code query} to activities whose {@code dateTime} falls within the given inclusive
   * range; either bound may be {@code null} to leave that side open, and both being {@code null}
   * leaves the query unchanged.
   */
  private void addDateRangeCriteria(Query query, LocalDateTime dateFrom, LocalDateTime dateTo) {
    if (dateFrom == null && dateTo == null) {
      return;
    }
    Criteria dates = Criteria.where("dateTime");
    if (dateFrom != null) {
      dates = dates.gte(dateFrom);
    }
    if (dateTo != null) {
      dates = dates.lte(dateTo);
    }
    query.addCriteria(dates);
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
  public Page<Activity> findActivitiesByOrganizerId(
      String organizerId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
    Query query = Query.query(Criteria.where("organizer").is(organizerId));
    addDateRangeCriteria(query, dateFrom, dateTo);
    return page(query, pageable);
  }

  @Override
  public Page<Activity> findActivitiesByParticipantId(
      String participantId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
    Query query = Query.query(Criteria.where("participants").is(participantId));
    addDateRangeCriteria(query, dateFrom, dateTo);
    return page(query, pageable);
  }

  @Override
  public boolean addParticipant(String activityId, String userId) {
    // Participants are stored as a list of user identifiers (document references).
    Query canJoin =
        Query.query(
            new Criteria()
                .andOperator(
                    Criteria.where("_id").is(activityId),
                    Criteria.where("participants").ne(userId),
                    availabilityCriteria(true)));
    boolean joined =
        mongoTemplate
                .updateFirst(canJoin, new Update().push("participants", userId), Activity.class)
                .getModifiedCount()
            == 1;
    searchTotals.invalidateAll();
    return joined;
  }

  @Override
  public void removeParticipant(String activityId, String userId) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(activityId)),
        new Update().pull("participants", userId),
        Activity.class);
    searchTotals.invalidateAll();
  }

  @Override
  public long assignTimeZoneWhereMissing(String timeZone) {
    Query withoutZone = Query.query(Criteria.where("timeZone").is(null));
    return mongoTemplate
        .updateMulti(withoutZone, Update.update("timeZone", timeZone), Activity.class)
        .getModifiedCount();
  }

  private Page<Activity> page(Query query, Pageable pageable) {
    long total = mongoTemplate.count(query, Activity.class);
    List<Activity> content = mongoTemplate.find(query.with(pageable), Activity.class);
    return new PageImpl<>(content, pageable, total);
  }

  /**
   * One page of an activity search, with three changes against {@link #page} that the load test
   * called for (docs/OBSERVABILIDAD.md):
   *
   * <ul>
   *   <li>The total is not counted when the page itself tells it (a short page, the last one), and
   *       otherwise it comes from {@link #searchTotals} when the same filter was counted recently.
   *   <li>The participants and organizers of the whole page are read with a single query, instead
   *       of one lazy lookup per activity (N+1).
   *   <li>Every access is timed as part of the search.
   * </ul>
   */
  private Page<Activity> searchPage(Query query, Pageable pageable, String filterKey) {
    Query pageQuery = Query.of(query).with(pageable);
    List<Document> documents =
        searchTelemetry.timeDatabase(
            "find",
            () ->
                mongoTemplate.find(
                    pageQuery, Document.class, mongoTemplate.getCollectionName(Activity.class)));
    List<Activity> content = withPeople(documents);
    return PageableExecutionUtils.getPage(content, pageable, () -> searchTotal(query, filterKey));
  }

  /**
   * The number of activities matching {@code query}, counted at most once per cache TTL. The key is
   * the search filter: the query itself has no stable text form (its availability condition is an
   * aggregation expression).
   */
  private long searchTotal(Query query, String filterKey) {
    Long cached = searchTotals.getIfPresent(filterKey);
    if (cached != null) {
      searchTelemetry.recordTotalSource("cache");
      return cached;
    }
    long total =
        searchTelemetry.timeDatabase("count", () -> mongoTemplate.count(query, Activity.class));
    searchTotals.put(filterKey, total);
    searchTelemetry.recordTotalSource("count");
    return total;
  }

  /**
   * Maps raw activity documents and attaches their participants and organizer, all read with one
   * query. A participant whose user no longer exists is left out, as the lazy references did; a
   * missing organizer keeps its identifier.
   */
  private List<Activity> withPeople(List<Document> documents) {
    Set<String> userIds = new LinkedHashSet<>();
    for (Document document : documents) {
      userIds.addAll(referencedIds(document.get("participants")));
      if (document.get("organizer") instanceof String organizerId) {
        userIds.add(organizerId);
      }
    }
    Map<String, User> users =
        userIds.isEmpty()
            ? Map.of()
            : searchTelemetry
                .timeDatabase(
                    "find_people",
                    () ->
                        mongoTemplate.find(
                            Query.query(Criteria.where("_id").in(userIds)), User.class))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

    return documents.stream()
        .map(
            document -> {
              Activity activity = mongoTemplate.getConverter().read(Activity.class, document);
              activity.setParticipants(
                  referencedIds(document.get("participants")).stream()
                      .map(users::get)
                      .filter(Objects::nonNull)
                      .toList());
              if (document.get("organizer") instanceof String organizerId) {
                activity.setOrganizer(users.getOrDefault(organizerId, User.withId(organizerId)));
              }
              return activity;
            })
        .toList();
  }

  private static List<String> referencedIds(Object references) {
    if (!(references instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }
}
