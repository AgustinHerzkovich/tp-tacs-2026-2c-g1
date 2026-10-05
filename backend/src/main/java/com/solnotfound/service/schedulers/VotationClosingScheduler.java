package com.solnotfound.service.schedulers;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.ActivityStatusTransitionService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class VotationClosingScheduler {

  private final IVotationRepository votationRepository;
  private final ActivityStatusTransitionService transitionService;
  private final boolean closingCheckOnStartup;
  private final String appMode;

  public VotationClosingScheduler(
      IVotationRepository votationRepository,
      ActivityStatusTransitionService transitionService,
      @Value("${votation.closing-check-on-startup:true}") boolean closingCheckOnStartup,
      @Value("${app.mode:}") String appMode) {
    this.votationRepository = votationRepository;
    this.transitionService = transitionService;
    this.closingCheckOnStartup = closingCheckOnStartup;
    this.appMode = appMode;
  }

  /**
   * Closes due votations and persists their activity outcome. Participation quorum is evaluated
   * across the whole votation; when reached, the most-voted option reschedules the activity.
   * Otherwise, the activity is cancelled. State is saved before notification publication.
   *
   * <p>A failure is logged and then rethrown so the caller (the cron pass or the Cloud Scheduler
   * HTTP trigger) can retry the whole pass later.
   */
  @Scheduled(cron = "${votation.closing-check-cron:0 0 * * * *}")
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
      justification = "Scheduler failures must propagate so Cloud Scheduler can retry the request")
  public void closeDueVotations() {
    // Closing dates are wall-clock times in each activity's zone. The query uses the latest
    // "now" on Earth so no due votation is missed; resolve() then checks the activity's own time.
    LocalDateTime latestNow = LocalDateTime.now(ZoneOffset.ofHours(14));
    var dueVotations = votationRepository.findActiveDueToClose(latestNow);
    int closed = 0;
    int failures = 0;
    log.info("Votation closing check started: dueVotations={}", dueVotations.size());
    for (Votation votation : dueVotations) {
      try {
        if (resolve(votation)) {
          closed++;
        }
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

  /**
   * Runs one closing pass as soon as the application is ready, so due votations do not have to wait
   * for the first cron execution. It reuses the same rules as the scheduled pass and runs
   * synchronously before the application reports itself as started.
   *
   * <p>The pass is skipped when {@code votation.closing-check-on-startup=false}, and also when
   * {@code app.mode=scheduled-jobs}: in that mode {@link ScheduledJobRunner} runs this very pass
   * from a {@code CommandLineRunner} and closes the context afterwards, so running it again here
   * would duplicate the work against an already closed context.
   *
   * <p>A failure of the pass is logged and swallowed: a database or scheduling problem must never
   * prevent the application from starting, and the cron execution retries later.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void closeDueVotationsOnStartup() {
    if (!closingCheckOnStartup || "scheduled-jobs".equals(appMode)) {
      log.info(
          "Startup votation closing check skipped: closingCheckOnStartup={} appMode={}",
          closingCheckOnStartup,
          appMode.isEmpty() ? "<unset>" : appMode);
      return;
    }
    try {
      closeDueVotations();
    } catch (RuntimeException exception) {
      log.error("Startup votation closing check failed", exception);
    }
  }

  private boolean resolve(Votation votation) {
    Activity activity = votation.getActivity();
    if (activity == null || votation.getStatus() != VotationStatus.ACTIVE) {
      log.warn(
          "Skipping invalid due votation: votationId={} activityId={} status={}",
          votation.getId(),
          activity == null ? null : activity.getId(),
          votation.getStatus());
      return false;
    }
    LocalDateTime now = activity.now();
    if (!votation.isDueToClose(now)) {
      return false;
    }

    int eligibleVoters =
        activity.getParticipants().size()
            + (activity.getOrganizer() != null && !activity.isAParticipant(activity.getOrganizer())
                ? 1
                : 0);
    ActivityStatus outcome;
    ActivityTransitionReason reason;
    if (votation.reachesQuorum(eligibleVoters)) {
      LocalDateTime winner = votation.winningOption(now).orElse(null);
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
    return true;
  }
}
