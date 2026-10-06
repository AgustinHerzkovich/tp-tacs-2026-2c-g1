package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.City;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.notification.Notification;
import com.solnotfound.entity.notification.StartingSoonNotificationType;
import com.solnotfound.entity.user.TelegramLinkCode;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.MaxRainProbabilityCondition;
import com.solnotfound.entity.weather.MaxWindCondition;
import com.solnotfound.entity.weather.TemperatureRangeCondition;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@DataMongoTest
@Testcontainers
class MongoPersistenceTest {

  @Container @ServiceConnection
  static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0.14");

  @Autowired private MongoActivityRepository mongoActivityRepository;
  @Autowired private MongoVotationRepository mongoVotationRepository;
  @Autowired private MongoNotificationRepository mongoNotificationRepository;
  @Autowired private MongoUserRepository mongoUserRepository;
  @Autowired private MongoTemplate mongoTemplate;

  private ActivityRepository activityRepository;
  private VotationRepository votationRepository;
  private NotificationRepository notificationRepository;
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().drop();
    activityRepository = new ActivityRepository(mongoActivityRepository, mongoTemplate);
    votationRepository = new VotationRepository(mongoVotationRepository, mongoTemplate);
    notificationRepository = new NotificationRepository(mongoNotificationRepository);
    userRepository = new UserRepository(mongoUserRepository);
  }

  @Test
  void persistsDocumentsReferencesEmbeddedValuesAndQueries() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    Activity activity = activity(organizer, participant);
    activityRepository.save(activity);

    Activity restored = activityRepository.findById(activity.getId());
    assertThat(restored.getOrganizer().getId()).isEqualTo("organizer");
    assertThat(restored.getParticipants()).extracting(User::getId).containsExactly("participant");
    assertThat(restored.getLocation().city().name()).isEqualTo("Córdoba");
    assertThat(restored.getWeatherConditions())
        .hasExactlyElementsOfTypes(
            MaxRainProbabilityCondition.class,
            TemperatureRangeCondition.class,
            MaxWindCondition.class);
    assertThat(activityRepository.findActivitiesByOrganizerId("organizer"))
        .extracting(Activity::getId)
        .containsExactly("activity-1");
    assertThat(activityRepository.findActivitiesByParticipantId("participant"))
        .extracting(Activity::getId)
        .containsExactly("activity-1");
    assertThat(activityRepository.findActive()).extracting(Activity::getId).contains("activity-1");

    LocalDateTime closingDate = LocalDateTime.of(2026, 9, 5, 20, 0);
    Votation votation = votation(activity, participant, closingDate);
    votationRepository.save(votation);
    Votation restoredVotation = votationRepository.findById(votation.getId());
    assertThat(restoredVotation.getActivity().getId()).isEqualTo("activity-1");
    assertThat(restoredVotation.getOptions().getFirst().getUsers())
        .extracting(User::getId)
        .containsExactly("participant");
    assertThat(votationRepository.findByActivityIds(List.of("activity-1")))
        .extracting(Votation::getId)
        .containsExactly(votation.getId());
    assertThat(votationRepository.findActiveDueToClose(closingDate))
        .extracting(Votation::getId)
        .containsExactly(votation.getId());

    Notification notification =
        new Notification(participant, activity, new StartingSoonNotificationType());
    notificationRepository.save(notification);
    assertThat(notificationRepository.findByReadAndReceiverUserId(false, "participant"))
        .singleElement()
        .satisfies(
            restoredNotification -> {
              assertThat(restoredNotification.getActivity().getId()).isEqualTo("activity-1");
              assertThat(restoredNotification.getReceiverUser().getId()).isEqualTo("participant");
              assertThat(restoredNotification.getType().code()).isEqualTo("STARTING_SOON");
            });

    Notification restoredNotification =
        notificationRepository.findById(notification.getId()).orElseThrow();
    restoredNotification.setAsRead();
    notificationRepository.save(restoredNotification);
    assertThat(notificationRepository.findByReadAndReceiverUserId(true, "participant"))
        .extracting(Notification::getId)
        .containsExactly(notification.getId());
  }

  @Test
  void filtersAvailableActivitiesUsingCapacityCalculatedFromPersistedFields() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    Activity available = activity(organizer, participant);
    activityRepository.save(available);

    Activity full = activity(organizer, participant);
    full.setId("activity-full");
    full.setMaxParticipants(1);
    activityRepository.save(full);

    assertThat(
            activityRepository
                .search(new ActivityFilterDTO(null, null, null, null, true), PageRequest.of(0, 10))
                .getContent())
        .extracting(Activity::getId)
        .containsExactly("activity-1");
  }

  @Test
  void searchesActivitiesByTitleIgnoringCase() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    activityRepository.save(activity(organizer, participant));
    Activity other = activity(organizer, participant);
    other.setId("activity-2");
    other.setTitle("Asado en la plaza");
    activityRepository.save(other);

    assertThat(
            activityRepository
                .search(
                    new ActivityFilterDTO(
                        null, null, null, null, null, List.of(), "  ASADO ", List.of()),
                    PageRequest.of(0, 10))
                .getContent())
        .extracting(Activity::getId)
        .containsExactly("activity-2");
  }

  @Test
  void searchesActivitiesByIdsAndStatus() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    activityRepository.save(activity(organizer, participant));
    Activity proposed = activity(organizer, participant);
    proposed.setId("activity-2");
    proposed.setStatus(ActivityStatus.PROPOSED);
    activityRepository.save(proposed);

    assertThat(
            activityRepository
                .search(
                    new ActivityFilterDTO(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(ActivityStatus.PROPOSED),
                        null,
                        List.of("activity-1", "activity-2")),
                    PageRequest.of(0, 10))
                .getContent())
        .extracting(Activity::getId)
        .containsExactly("activity-2");
  }

  @Test
  void searchesVotationsByStatusAndOwnVoteUsingStoredReferences() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    Activity activity = activity(organizer, participant);
    activityRepository.save(activity);
    LocalDateTime closingDate = LocalDateTime.of(2026, 9, 5, 20, 0);
    Votation active = votation(activity, participant, closingDate);
    votationRepository.save(active);
    Votation closed = votation(activity, participant, closingDate);
    closed.setStatus(VotationStatus.CLOSED);
    closed.setCreationDate(closingDate.minusDays(2));
    votationRepository.save(closed);
    List<String> activityIds = List.of("activity-1");
    PageRequest page = PageRequest.of(0, 10);

    assertThat(
            votationRepository
                .search(
                    activityIds,
                    new VotationFilterDTO(VotationStatus.ACTIVE, null, true),
                    "participant",
                    page)
                .getContent())
        .extracting(Votation::getId)
        .containsExactly(active.getId());
    assertThat(
            votationRepository
                .search(activityIds, new VotationFilterDTO(null, null, false), "organizer", page)
                .getContent())
        .extracting(Votation::getId)
        .containsExactly(active.getId(), closed.getId());
    assertThat(
            votationRepository
                .search(activityIds, new VotationFilterDTO(null, null, false), "participant", page)
                .getContent())
        .isEmpty();
  }

  @Test
  void addsAndRemovesParticipantsAtomicallyRespectingCapacityAndStatus() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    userRepository.findOrCreate("second");
    userRepository.findOrCreate("third");
    Activity activity = activity(organizer, participant);
    activity.setMaxParticipants(2);
    activityRepository.save(activity);

    assertThat(activityRepository.addParticipant("activity-1", "participant")).isFalse();
    assertThat(activityRepository.addParticipant("activity-1", "second")).isTrue();
    assertThat(activityRepository.addParticipant("activity-1", "third")).isFalse();
    assertThat(activityRepository.findById("activity-1").getParticipants())
        .extracting(User::getId)
        .containsExactly("participant", "second");

    activityRepository.removeParticipant("activity-1", "participant");
    activityRepository.removeParticipant("activity-1", "not-a-participant");
    assertThat(activityRepository.findById("activity-1").getParticipants())
        .extracting(User::getId)
        .containsExactly("second");

    Activity cancelled = activityRepository.findById("activity-1");
    cancelled.setStatus(ActivityStatus.CANCELLED);
    activityRepository.save(cancelled);
    assertThat(activityRepository.addParticipant("activity-1", "third")).isFalse();
  }

  @Test
  void neverExceedsCapacityWhenManyUsersJoinAtTheSameTime() throws Exception {
    User organizer = userRepository.findOrCreate("organizer");
    Activity activity = activity(organizer, organizer);
    activity.setParticipants(List.of());
    activity.setMaxParticipants(5);
    activityRepository.save(activity);
    int users = 40;
    for (int index = 0; index < users; index++) {
      userRepository.findOrCreate("user-" + index);
    }

    ExecutorService pool = Executors.newFixedThreadPool(16);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> attempts = new ArrayList<>();
    for (int index = 0; index < users; index++) {
      String userId = "user-" + index;
      attempts.add(
          pool.submit(
              () -> {
                start.await();
                return activityRepository.addParticipant("activity-1", userId);
              }));
    }
    start.countDown();
    int joined = 0;
    for (Future<Boolean> attempt : attempts) {
      if (attempt.get(30, TimeUnit.SECONDS)) {
        joined++;
      }
    }
    pool.shutdown();

    assertThat(joined).isEqualTo(5);
    assertThat(activityRepository.findById("activity-1").getParticipants()).hasSize(5);
  }

  @Test
  void assignsTimeZoneOnlyToActivitiesThatHaveNone() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    activityRepository.save(activity(organizer, participant));
    Activity withZone = activity(organizer, participant);
    withZone.setId("activity-2");
    withZone.setTimeZone("Europe/Madrid");
    activityRepository.save(withZone);

    long updated = activityRepository.assignTimeZoneWhereMissing("America/Argentina/Buenos_Aires");

    assertThat(updated).isEqualTo(1);
    assertThat(activityRepository.findById("activity-1").getTimeZone())
        .isEqualTo("America/Argentina/Buenos_Aires");
    assertThat(activityRepository.findById("activity-2").getTimeZone()).isEqualTo("Europe/Madrid");
    assertThat(activityRepository.assignTimeZoneWhereMissing("UTC")).isZero();
  }

  @Test
  void redeemsTelegramLinkCodesOnceAndNeverAfterTheyExpire() {
    TelegramLinkCodeRepository linkCodes = new TelegramLinkCodeRepository(mongoTemplate);
    Instant now = Instant.parse("2026-09-29T12:00:00Z");
    linkCodes.save(new TelegramLinkCode("valid-hash", 123L, now.plusSeconds(600)));
    linkCodes.save(new TelegramLinkCode("expired-hash", 456L, now.minusSeconds(1)));

    assertThat(linkCodes.consume("valid-hash", now)).map(TelegramLinkCode::chatId).contains(123L);
    assertThat(linkCodes.consume("valid-hash", now)).isEmpty();
    assertThat(linkCodes.consume("expired-hash", now)).isEmpty();
  }

  private Activity activity(User organizer, User participant) {
    Activity activity = new Activity();
    activity.setId("activity-1");
    activity.setTitle("Football match");
    activity.setDescription("Weekly match");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Córdoba"), -31.42, -64.18));
    activity.setDateTime(LocalDateTime.of(2026, 9, 6, 18, 0));
    activity.setMinParticipants(2);
    activity.setMaxParticipants(10);
    activity.setOrganizer(organizer);
    activity.setParticipants(List.of(participant));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(40),
            new TemperatureRangeCondition(10, 30),
            new MaxWindCondition(25.0)));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(9, 0), LocalTime.of(21, 0)));
    return activity;
  }

  @Test
  void rejectsSavingAVotationOverAStateThatSomeoneElseAlreadyReplaced() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    Activity activity = activity(organizer, participant);
    activityRepository.save(activity);
    Votation created =
        votationRepository.save(
            votation(activity, participant, LocalDateTime.of(2026, 9, 5, 12, 0)));
    Votation first = votationRepository.findById(created.getId());
    Votation second = votationRepository.findById(created.getId());

    first.setMinQuorum(0.8);
    votationRepository.save(first);
    second.setStatus(VotationStatus.CLOSED);

    assertThatThrownBy(() -> votationRepository.save(second))
        .isInstanceOf(OptimisticLockingFailureException.class);
    Votation stored = votationRepository.findById(created.getId());
    assertThat(stored.getStatus()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(stored.getMinQuorum()).isEqualTo(0.8);

    stored.setStatus(VotationStatus.CLOSED);
    votationRepository.save(stored);
    assertThat(votationRepository.findById(created.getId()).getStatus())
        .isEqualTo(VotationStatus.CLOSED);
  }

  @Test
  void savesVotationsStoredBeforeTheVersionFieldExisted() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    Activity activity = activity(organizer, participant);
    activityRepository.save(activity);
    Votation created =
        votationRepository.save(
            votation(activity, participant, LocalDateTime.of(2026, 9, 5, 12, 0)));
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(created.getId())),
        new Update().unset("version"),
        Votation.class);

    Votation legacy = votationRepository.findById(created.getId());
    legacy.setMinQuorum(0.3);
    votationRepository.save(legacy);

    Votation stored = votationRepository.findById(created.getId());
    assertThat(stored.getMinQuorum()).isEqualTo(0.3);
    assertThat(stored.getVersion()).isEqualTo(1);
  }

  @Test
  void removesTheVotesOfAUserOnlyFromActiveVotationsOfTheActivity() {
    User organizer = userRepository.findOrCreate("organizer");
    User participant = userRepository.findOrCreate("participant");
    User other = userRepository.findOrCreate("other");
    Activity activity = activity(organizer, participant);
    activityRepository.save(activity);
    LocalDateTime closingDate = LocalDateTime.of(2026, 9, 5, 12, 0);
    Votation active = votation(activity, participant, closingDate);
    VotationOption second = new VotationOption();
    second.setDateTime(activity.getDateTime().plusDays(2));
    second.setUsers(List.of(other));
    active.setOptions(List.of(active.getOptions().getFirst(), second));
    active = votationRepository.save(active);
    Votation closed = votation(activity, participant, closingDate);
    closed.setStatus(VotationStatus.CLOSED);
    closed = votationRepository.save(closed);
    Votation readBeforeRemoval = votationRepository.findById(active.getId());

    votationRepository.removeVotes("activity-1", "participant");

    Votation stored = votationRepository.findById(active.getId());
    assertThat(stored.getVoteByUserId("participant")).isEmpty();
    assertThat(stored.getVoteByUserId("other")).isPresent();
    assertThat(votationRepository.findById(closed.getId()).getVoteByUserId("participant"))
        .isPresent();
    // A save made over the state read before the removal must not bring the vote back.
    assertThatThrownBy(() -> votationRepository.save(readBeforeRemoval))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  private Votation votation(Activity activity, User participant, LocalDateTime closingDate) {
    VotationOption option = new VotationOption();
    option.setDateTime(activity.getDateTime().plusDays(1));
    option.setUsers(List.of(participant));

    Votation votation = new Votation();
    votation.setActivity(activity);
    votation.setCreationDate(closingDate.minusHours(1));
    votation.setClosingDate(closingDate);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setOptions(List.of(option));
    return votation;
  }
}
