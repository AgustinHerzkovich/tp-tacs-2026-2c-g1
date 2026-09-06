package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.City;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.notification.Notification;
import com.solnotfound.entity.notification.StartingSoonNotificationType;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.MaxRainProbabilityCondition;
import com.solnotfound.entity.weather.MaxWindCondition;
import com.solnotfound.entity.weather.TemperatureRangeCondition;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    activityRepository = new ActivityRepository(mongoActivityRepository);
    votationRepository = new VotationRepository(mongoVotationRepository);
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
