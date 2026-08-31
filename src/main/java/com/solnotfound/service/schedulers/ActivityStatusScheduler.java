package com.solnotfound.service.schedulers;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.StartingSoonNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.service.ActivityStatusTransitionService;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects the shared in-memory repository")
public class ActivityStatusScheduler {

  private final IActivityRepository activityRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ActivityStatusTransitionService transitionService;
  private final Duration notificationThreshold;

  public ActivityStatusScheduler(
      IActivityRepository activityRepository,
      ApplicationEventPublisher eventPublisher,
      ActivityStatusTransitionService transitionService,
      @Value("${activity.starting-soon.threshold:1h}") Duration notificationThreshold) {
    this.activityRepository = activityRepository;
    this.eventPublisher = eventPublisher;
    this.transitionService = transitionService;
    this.notificationThreshold = notificationThreshold;
  }

  /**
   * Validates the status of all active activities once per hour. If an activity is near its start
   * time, a notification is sent to its participants. If an activity is finished, its status is
   * updated accordingly.
   */
  @Scheduled(fixedDelayString = "${activity.status-check-interval:5m}")
  public void finishPastActivities() {
    LocalDateTime now = LocalDateTime.now();

    for (Activity activity : activityRepository.findActive()) {
      try {
        if (activity.getDateTime().isBefore(now)) {
          transitionService.transition(activity, ActivityStatus.FINISHED);
        } else if (activity.nearStart(now, notificationThreshold)
            && !activity.wasStartingSoonNotificationSent()) {
          eventPublisher.publishEvent(
              ActivityNotificationEvent.from(activity, new StartingSoonNotificationType()));
          activity.markStartingSoonNotificationSent();
          activityRepository.save(activity);
        }
      } catch (RuntimeException exception) {
        log.error(
            "Could not process status for activity {}: {}",
            activity.getId(),
            exception.getMessage());
      }
    }
  }
}
