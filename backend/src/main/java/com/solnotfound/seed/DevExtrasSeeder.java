package com.solnotfound.seed;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.BadWeatherAlertNotificationType;
import com.solnotfound.entity.notification.ReprogrammedNotificationType;
import com.solnotfound.entity.notification.StartingSoonNotificationType;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.NotificationService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * One-off dev convenience: opens a votation and generates a batch of notifications on an existing
 * activity organized by (or with) a specific real Keycloak user, so that flow is visible in the UI
 * without waiting for the hourly weather-check scheduler that normally triggers it.
 *
 * <p>Disabled by default and separate from {@link ActivitySeeder} — it targets a real user's
 * already-existing activity rather than fixed fake data, so it needs that user's id up front.
 * Enable for one run with {@code app.dev-seed.enabled=true} and {@code
 * app.dev-seed.user-id=<keycloak sub>} (e.g. {@code APP_DEV_SEED_ENABLED=true
 * APP_DEV_SEED_USER_ID=... docker compose up -d --build backend}), then turn it back off — unlike
 * {@link ActivitySeeder}, the votation uses a fixed id and existing votations are never
 * overwritten. Notifications are generated only when the seeded votation is created.
 */
@Component
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
public class DevExtrasSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DevExtrasSeeder.class);

  private final IActivityRepository activityRepository;
  private final IUserRepository userRepository;
  private final IVotationRepository votationRepository;
  private final NotificationService notificationService;
  private final String targetUserId;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared repository/service beans")
  public DevExtrasSeeder(
      IActivityRepository activityRepository,
      IUserRepository userRepository,
      IVotationRepository votationRepository,
      NotificationService notificationService,
      @Value("${app.dev-seed.user-id:}") String targetUserId) {
    this.activityRepository = activityRepository;
    this.userRepository = userRepository;
    this.votationRepository = votationRepository;
    this.notificationService = notificationService;
    this.targetUserId = targetUserId;
  }

  @Override
  public void run(String... args) {
    if (targetUserId == null || targetUserId.isBlank()) {
      log.warn(
          "app.dev-seed.enabled=true but app.dev-seed.user-id is not set — skipping. Set it to"
              + " the Keycloak user id (JWT sub) you're logged in as.");
      return;
    }

    userRepository.findOrCreate(targetUserId);
    Activity activity = findAnActivityFor(targetUserId);
    if (activity == null) {
      log.warn(
          "app.dev-seed: no activity organized by or including participant {} was found — create"
              + " one through the app first, then re-run.",
          targetUserId);
      return;
    }

    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
    if (!seedVotation(activity, now)) {
      return;
    }
    seedNotifications(activity);

    log.info(
        "app.dev-seed complete: votation + notifications attached to activity '{}' ({}) for user"
            + " {}.",
        activity.getTitle(),
        activity.getId(),
        targetUserId);
  }

  /** Prefers an activity the user organizes; falls back to one they only participate in. */
  private Activity findAnActivityFor(String userId) {
    Map<String, Activity> candidates = new LinkedHashMap<>();
    for (Activity activity : activityRepository.findActivitiesByOrganizerId(userId)) {
      candidates.putIfAbsent(activity.getId(), activity);
    }
    for (Activity activity : activityRepository.findActivitiesByParticipantId(userId)) {
      candidates.putIfAbsent(activity.getId(), activity);
    }
    return candidates.values().stream()
        .filter(
            activity ->
                activity.getStatus() != ActivityStatus.CANCELLED
                    && activity.getStatus() != ActivityStatus.FINISHED)
        .findFirst()
        .orElse(null);
  }

  private boolean seedVotation(Activity activity, LocalDateTime now) {
    String votationId = "dev-votation-" + activity.getId();
    if (votationRepository.findById(votationId) != null
        || votationRepository.findActiveByActivityId(activity.getId()) != null) {
      log.info("app.dev-seed: activity {} already has a votation, skipping.", activity.getId());
      return false;
    }

    List<VotationOption> options = buildOptions(activity);
    if (options.size() < 3) {
      log.warn(
          "app.dev-seed: activity {} does not allow three distinct reprogramming options,"
              + " skipping.",
          activity.getId());
      return false;
    }

    Votation votation = new Votation();
    votation.setId(votationId);
    votation.setActivity(activity);
    votation.setCreationDate(now);
    votation.setClosingDate(now.plusDays(1));
    votation.setMinQuorum(0.5);
    votation.setStatus(VotationStatus.ACTIVE);

    votation.setOptions(options);

    votationRepository.save(votation);

    activity.setStatus(ActivityStatus.PROPOSED);
    activityRepository.save(activity);
    return true;
  }

  private List<VotationOption> buildOptions(Activity activity) {
    var range = activity.getReprogramationRange();
    if (range == null) {
      return List.of();
    }

    LocalDateTime original = activity.getDateTime();
    return IntStream.rangeClosed(1, range.getMaxDays())
        .mapToObj(day -> original.toLocalDate().plusDays(day).atTime(range.getInitialHour()))
        .filter(candidate -> range.isWithinRange(original, candidate))
        .distinct()
        .limit(3)
        .map(this::optionAt)
        .toList();
  }

  private VotationOption optionAt(LocalDateTime dateTime) {
    VotationOption option = new VotationOption();
    option.setDateTime(dateTime);
    option.setUsers(List.of());
    return option;
  }

  private void seedNotifications(Activity activity) {
    notificationService.generateNotificationsForActivityEvent(
        activity, new BadWeatherAlertNotificationType());
    notificationService.generateNotificationsForActivityEvent(
        activity, new ReprogrammedNotificationType());
    notificationService.generateNotificationsForActivityEvent(
        activity, new StartingSoonNotificationType());
  }
}
