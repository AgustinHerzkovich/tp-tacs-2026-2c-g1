package com.solnotfound.service.schedulers;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.ActivityStatusTransitionService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VotationClosingScheduler {

  private final IVotationRepository votationRepository;
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
    Activity activity = votation.getActivity();
    if (activity == null || votation.getStatus() != VotationStatus.ACTIVE) {
      return;
    }

    int eligibleVoters =
        activity.getParticipants().size()
            + (activity.getOrganizer() != null && !activity.isAParticipant(activity.getOrganizer())
                ? 1
                : 0);
    ActivityStatus outcome;
    ActivityTransitionReason reason;
    if (votation.reachesQuorum(eligibleVoters)) {
      LocalDateTime winner = votation.winningOption().orElse(null);
      if (winner == null) {
        outcome = ActivityStatus.CANCELLED;
        reason = ActivityTransitionReason.VOTATION_WITHOUT_WINNER;
      } else {
        activity.setDateTime(winner);
        outcome = ActivityStatus.RESCHEDULED;
        reason = ActivityTransitionReason.VOTATION_RESOLVED;
      }
    } else {
      outcome = ActivityStatus.CANCELLED;
      reason = ActivityTransitionReason.QUORUM_NOT_REACHED;
    }

    votation.setStatus(VotationStatus.CLOSED);
    votationRepository.save(votation);
    transitionService.transition(activity, outcome, reason);
  }
}
