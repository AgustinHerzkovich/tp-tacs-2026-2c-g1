package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import com.solnotfound.service.schedulers.ActivityStatusScheduler;
import com.solnotfound.service.schedulers.ScheduledJobRunner;
import com.solnotfound.service.schedulers.VotationClosingScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ConfigurableApplicationContext;

class ScheduledJobRunnerTest {

  private final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
  private final ActivityAnticipationCheckScheduler anticipation =
      mock(ActivityAnticipationCheckScheduler.class);
  private final ActivityStatusScheduler activityStatus = mock(ActivityStatusScheduler.class);
  private final VotationClosingScheduler votations = mock(VotationClosingScheduler.class);
  private final ScheduledJobRunner runner =
      new ScheduledJobRunner(context, anticipation, activityStatus, votations);

  @Test
  void runsRequestedJobsInOrderAndClosesContext() {
    runner.run(
        "--spring.main.web-application-type=none", "weather", "votations", "activity-status");

    InOrder order = inOrder(anticipation, votations, activityStatus, context);
    order.verify(anticipation).checkActivitiesClimate();
    order.verify(votations).closeDueVotations();
    order.verify(activityStatus).finishPastActivities();
    order.verify(context).close();
  }

  @Test
  void closesContextWhenJobNameIsUnknown() {
    assertThatThrownBy(() -> runner.run("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown scheduled job: unknown");

    verify(context).close();
  }
}
