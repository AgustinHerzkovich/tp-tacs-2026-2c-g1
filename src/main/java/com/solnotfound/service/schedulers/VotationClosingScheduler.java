package com.solnotfound.service.schedulers;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.entity.notifications.CancelledNotificationType;
import com.solnotfound.entity.notifications.NotificationType;
import com.solnotfound.entity.notifications.ReprogrammedNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class VotationClosingScheduler {

  private final IVotationRepository votationRepository;
  private final IActivityRepository activityRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Closes due votations and persists their activity outcome. Participation quorum is evaluated
   * across the whole votation; when reached, the most-voted option reschedules the activity.
   * Otherwise, the activity is cancelled. State is saved before notification publication.
   */
  @Scheduled(fixedDelayString = "${votation.closing-check-interval:1h}")
  public void closeDueVotations() {
    LocalDateTime now = LocalDateTime.now();
    for (Votation votation : votationRepository.findActiveDueToClose(now)) {
      resolve(votation);
    }
  }

  private void resolve(Votation votation) {
    Activity activity = activityRepository.findById(votation.getActivityId());
    if (activity == null || votation.getStatus() != VotationStatus.ACTIVE) {
      return;
    }

    int eligibleVoters =
        activity.getParticipants().size()
            + (activity.getOrganizer() != null && !activity.isAParticipant(activity.getOrganizer())
                ? 1
                : 0);
    NotificationType notificationType;
    if (votation.reachesQuorum(eligibleVoters)) {
      LocalDateTime winner = votation.winningOption().orElse(null);
      if (winner == null) {
        activity.setStatus(ActivityStatus.CANCELLED);
        notificationType = new CancelledNotificationType();
      } else {
        activity.setDateTime(winner);
        activity.setStatus(ActivityStatus.RESCHEDULED);
        notificationType = new ReprogrammedNotificationType();
      }
    } else {
      activity.setStatus(ActivityStatus.CANCELLED);
      notificationType = new CancelledNotificationType();
    }

    votation.setStatus(VotationStatus.CLOSED);
    votationRepository.save(votation);
    activityRepository.save(activity);
    eventPublisher.publishEvent(ActivityNotificationEvent.from(activity, notificationType));
  }
}
