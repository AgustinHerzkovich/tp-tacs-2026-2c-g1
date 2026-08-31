package com.solnotfound.service.schedulers;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.ActivityStatusTransitionService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
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
  private final ActivityStatusTransitionService transitionService;

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
    ActivityStatus outcome;
    if (votation.reachesQuorum(eligibleVoters)) {
      LocalDateTime winner = votation.winningOption().orElse(null);
      if (winner == null) {
        outcome = ActivityStatus.CANCELLED;
      } else {
        activity.setDateTime(winner);
        outcome = ActivityStatus.RESCHEDULED;
      }
    } else {
      outcome = ActivityStatus.CANCELLED;
    }

    votation.setStatus(VotationStatus.CLOSED);
    votationRepository.save(votation);
    transitionService.transition(activity, outcome);
  }
}
