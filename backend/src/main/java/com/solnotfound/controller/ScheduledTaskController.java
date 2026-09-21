package com.solnotfound.controller;

import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import com.solnotfound.service.schedulers.VotationClosingScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal HTTP entry points invoked by Cloud Scheduler. */
@RestController
@RequestMapping("/internal/scheduled")
@RequiredArgsConstructor
public class ScheduledTaskController {

  private final ActivityAnticipationCheckScheduler anticipationScheduler;
  private final ActivityStatusScheduler statusScheduler;
  private final VotationClosingScheduler votationScheduler;

  /** Runs the weather anticipation maintenance pass synchronously. */
  @PostMapping("/weather")
  public ResponseEntity<Void> checkWeather() {
    anticipationScheduler.checkActivitiesClimate();
    return ResponseEntity.noContent().build();
  }

  /** Runs the activity status maintenance pass synchronously. */
  @PostMapping("/activity-status")
  public ResponseEntity<Void> updateActivityStatuses() {
    statusScheduler.finishPastActivities();
    return ResponseEntity.noContent().build();
  }

  /** Runs the due-votation closing pass synchronously. */
  @PostMapping("/votations")
  public ResponseEntity<Void> closeVotations() {
    votationScheduler.closeDueVotations();
    return ResponseEntity.noContent().build();
  }
}
