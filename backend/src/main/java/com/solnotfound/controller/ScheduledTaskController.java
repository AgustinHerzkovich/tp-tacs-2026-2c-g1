package com.solnotfound.controller;

import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import com.solnotfound.service.schedulers.VotationClosingScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal HTTP entry points invoked by Cloud Scheduler. */
@RestController
@RequestMapping("/internal/scheduled")
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskController {

  private final ActivityAnticipationCheckScheduler anticipationScheduler;
  private final ActivityStatusScheduler statusScheduler;
  private final VotationClosingScheduler votationScheduler;

  /** Runs the weather anticipation maintenance pass synchronously. */
  @PostMapping("/weather")
  public ResponseEntity<Void> checkWeather() {
    runTask("weather", anticipationScheduler::checkActivitiesClimate);
    return ResponseEntity.noContent().build();
  }

  /** Runs the activity status maintenance pass synchronously. */
  @PostMapping("/activity-status")
  public ResponseEntity<Void> updateActivityStatuses() {
    runTask("activity-status", statusScheduler::finishPastActivities);
    return ResponseEntity.noContent().build();
  }

  /** Runs the due-votation closing pass synchronously. */
  @PostMapping("/votations")
  public ResponseEntity<Void> closeVotations() {
    runTask("votations", votationScheduler::closeDueVotations);
    return ResponseEntity.noContent().build();
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
      justification =
          "Scheduler failures must propagate so Cloud Scheduler receives a failed response")
  private void runTask(String taskName, Runnable task) {
    long startedAt = System.nanoTime();
    log.info("Scheduled task started: task={}", taskName);
    try {
      task.run();
      log.info(
          "Scheduled task completed: task={} durationMs={}",
          taskName,
          (System.nanoTime() - startedAt) / 1_000_000);
    } catch (RuntimeException exception) {
      log.error(
          "Scheduled task failed: task={} durationMs={}",
          taskName,
          (System.nanoTime() - startedAt) / 1_000_000,
          exception);
      throw exception;
    }
  }
}
