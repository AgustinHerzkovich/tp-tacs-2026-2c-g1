package com.solnotfound.service.schedulers;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.notifications.StartedNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
  value = "EI_EXPOSE_REP2",
  justification = "Spring injects the shared in-memory repository")
public class ActivityStatusScheduler {

  private final IActivityRepository activityRepository;
  private final ApplicationEventPublisher eventPublisher;


  /**
   * Validates the status of all active activities once per hour. If an activity is near its start time, a notification is sent to its participants.
   * If an activity is finished, its status is updated accordingly.
   */
  @Scheduled(cron = "0 0 * * * *")
  public void finishPastActivities() {
    LocalDateTime now = LocalDateTime.now();

    int notificationThreshold = 1; //TODO: recuperar de un property el tiempo de anticipación para notificar a los participantes

    for (Activity activity : activityRepository.findActive()) {
      if (activity.finishIfPast(now)) {
        activityRepository.save(activity);
      } else if (activity.nearStart(now, notificationThreshold)) {
        eventPublisher.publishEvent(
          ActivityNotificationEvent.from(activity, new StartedNotificationType()));
      }
    }
  }
}

