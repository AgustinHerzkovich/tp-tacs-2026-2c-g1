package com.solnotfound.service;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.notification.CancelledNotificationType;
import com.solnotfound.entity.notification.NotificationType;
import com.solnotfound.entity.notification.ReprogrammedNotificationType;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.exception.InvalidActivityStatusTransitionException;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.InMemoryStatisticsEventRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class ActivityStatusTransitionService {

  private static final Map<ActivityStatus, Set<ActivityStatus>> ALLOWED_TRANSITIONS =
      allowedTransitions();

  private final IActivityRepository activityRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final StatisticsEventRecorder statisticsRecorder;

  @Autowired
  public ActivityStatusTransitionService(
      IActivityRepository activityRepository,
      ApplicationEventPublisher eventPublisher,
      StatisticsEventRecorder statisticsRecorder) {
    this.activityRepository = activityRepository;
    this.eventPublisher = eventPublisher;
    this.statisticsRecorder = statisticsRecorder;
  }

  public ActivityStatusTransitionService(
      IActivityRepository activityRepository, ApplicationEventPublisher eventPublisher) {
    this(
        activityRepository,
        eventPublisher,
        new StatisticsEventRecorder(new InMemoryStatisticsEventRepository()));
  }

  public void transition(Activity activity, ActivityStatus newStatus) {
    transition(activity, newStatus, null);
  }

  /**
   * Validates and persists an activity status transition, then publishes the notification
   * associated with the destination state. Cancellation and rescheduling notify interested users;
   * proposed and finished states only persist the transition.
   *
   * @param activity activity to transition
   * @param newStatus destination status
   * @param reason business reason for the transition, when relevant
   * @throws InvalidActivityStatusTransitionException when the transition is not allowed
   */
  public void transition(
      Activity activity, ActivityStatus newStatus, ActivityTransitionReason reason) {
    ActivityStatus currentStatus = activity.getStatus();
    if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
      throw new InvalidActivityStatusTransitionException(
          "Activity cannot transition from " + currentStatus + " to " + newStatus);
    }

    activity.setStatus(newStatus);
    activityRepository.save(activity);

    StatisticsEventType statisticsType = statisticsTypeFor(newStatus);
    if (statisticsType != null) {
      statisticsRecorder.recordActivity(statisticsType, activity.getId(), reason);
    }

    NotificationType notificationType = notificationFor(newStatus);
    if (notificationType != null) {
      eventPublisher.publishEvent(ActivityNotificationEvent.from(activity, notificationType));
    }
  }

  private StatisticsEventType statisticsTypeFor(ActivityStatus status) {
    return switch (status) {
      case CANCELLED -> StatisticsEventType.ACTIVITY_CANCELLED;
      case RESCHEDULED -> StatisticsEventType.ACTIVITY_RESCHEDULED;
      default -> null;
    };
  }

  private static Map<ActivityStatus, Set<ActivityStatus>> allowedTransitions() {
    Map<ActivityStatus, Set<ActivityStatus>> transitions = new EnumMap<>(ActivityStatus.class);
    transitions.put(
        ActivityStatus.CONFIRMED,
        Set.of(ActivityStatus.PROPOSED, ActivityStatus.CANCELLED, ActivityStatus.FINISHED));
    transitions.put(
        ActivityStatus.PROPOSED, Set.of(ActivityStatus.RESCHEDULED, ActivityStatus.CANCELLED));
    transitions.put(
        ActivityStatus.RESCHEDULED,
        Set.of(ActivityStatus.PROPOSED, ActivityStatus.CANCELLED, ActivityStatus.FINISHED));
    transitions.put(ActivityStatus.CANCELLED, Set.of());
    transitions.put(ActivityStatus.FINISHED, Set.of());
    return Map.copyOf(transitions);
  }

  private NotificationType notificationFor(ActivityStatus status) {
    return switch (status) {
      case CANCELLED -> new CancelledNotificationType();
      case RESCHEDULED -> new ReprogrammedNotificationType();
      default -> null;
    };
  }
}
