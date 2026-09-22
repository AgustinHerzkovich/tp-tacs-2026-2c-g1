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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class VotationClosingScheduler {

  private final IVotationRepository votationRepository;
  private final ActivityStatusTransitionService transitionService;

  /**
   * Closes due votations and persists their activity outcome. Participation quorum is evaluated
   * across the whole votation; when reached, the most-voted option reschedules the activity.
   * Otherwise, the activity is cancelled. State is saved before notification publication.
   */
  @Scheduled(cron = "${votation.closing-check-cron:0 0 * * * *}")
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
      justification = "Scheduler failures must propagate so Cloud Scheduler can retry the request")
  public void closeDueVotations() {
    LocalDateTime now = LocalDateTime.now();
    var dueVotations = votationRepository.findActiveDueToClose(now);
    int closed = 0;
    int failures = 0;
    log.info("Votation closing check started: dueVotations={}", dueVotations.size());
    for (Votation votation : dueVotations) {
      try {
        resolve(votation);
        closed++;
      } catch (RuntimeException exception) {
        failures++;
        log.error(
            "Could not close votation: votationId={} activityId={}",
            votation.getId(),
            votation.getActivity() == null ? null : votation.getActivity().getId(),
            exception);
        throw exception;
      }
    }
    log.info(
        "Votation closing check completed: dueVotations={} closed={} failures={}",
        dueVotations.size(),
        closed,
        failures);
  }

  private void resolve(Votation votation) {
    Activity activity = votation.getActivity();
    if (activity == null || votation.getStatus() != VotationStatus.ACTIVE) {
      log.warn(
          "Skipping invalid due votation: votationId={} activityId={} status={}",
          votation.getId(),
          activity == null ? null : activity.getId(),
          votation.getStatus());
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
    log.info(
        "Votation closed: votationId={} activityId={} outcome={} reason={} eligibleVoters={}",
        votation.getId(),
        activity.getId(),
        outcome,
        reason,
        eligibleVoters);
  }
}
