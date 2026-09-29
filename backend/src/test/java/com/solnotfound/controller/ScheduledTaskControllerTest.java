package com.solnotfound.controller;

import static org.mockito.Mockito.verify;

import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import com.solnotfound.service.schedulers.VotationClosingScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ScheduledTaskControllerTest {

  private final ActivityAnticipationCheckScheduler anticipationScheduler =
      org.mockito.Mockito.mock(ActivityAnticipationCheckScheduler.class);
  private final ActivityStatusScheduler statusScheduler =
      org.mockito.Mockito.mock(ActivityStatusScheduler.class);
  private final VotationClosingScheduler votationScheduler =
      org.mockito.Mockito.mock(VotationClosingScheduler.class);
  private final ScheduledTaskController controller =
      new ScheduledTaskController(anticipationScheduler, statusScheduler, votationScheduler);

  @Test
  void invokesWeatherPass() {
    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.NO_CONTENT, controller.checkWeather().getStatusCode());
    verify(anticipationScheduler).checkActivitiesClimate();
  }

  @Test
  void invokesActivityStatusPass() {
    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.NO_CONTENT, controller.updateActivityStatuses().getStatusCode());
    verify(statusScheduler).finishPastActivities();
  }

  @Test
  void invokesVotationPass() {
    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.NO_CONTENT, controller.closeVotations().getStatusCode());
    verify(votationScheduler).closeDueVotations();
  }
}
