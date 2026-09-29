package com.solnotfound.service.schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Executes one or more scheduler passes in a finite Cloud Run Job process. */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "scheduled-jobs")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects the shared application context used to terminate the job")
public class ScheduledJobRunner implements CommandLineRunner {

  private final ConfigurableApplicationContext context;
  private final Map<String, Consumer<Void>> jobs;

  public ScheduledJobRunner(
      ConfigurableApplicationContext context,
      ActivityAnticipationCheckScheduler anticipationScheduler,
      ActivityStatusScheduler statusScheduler,
      VotationClosingScheduler votationScheduler) {
    this.context = context;
    this.jobs =
        Map.of(
            "weather", ignored -> anticipationScheduler.checkActivitiesClimate(),
            "activity-status", ignored -> statusScheduler.finishPastActivities(),
            "votations", ignored -> votationScheduler.closeDueVotations());
  }

  /**
   * Runs the named jobs in argument order and terminates Spring after completion.
   *
   * @param args job names: {@code weather}, {@code activity-status}, or {@code votations}; when
   *     omitted all jobs run once
   * @throws IllegalArgumentException when a name is unknown
   */
  @Override
  public void run(String... args) {
    List<String> positionalArgs = Arrays.stream(args).filter(arg -> !arg.startsWith("--")).toList();
    String[] requested =
        positionalArgs.isEmpty()
            ? jobs.keySet().toArray(String[]::new)
            : positionalArgs.toArray(String[]::new);
    try {
      Arrays.stream(requested)
          .map(name -> name.toLowerCase(Locale.ROOT))
          .forEach(
              name -> {
                Consumer<Void> job = jobs.get(name);
                if (job == null) {
                  throw new IllegalArgumentException("Unknown scheduled job: " + name);
                }
                job.accept(null);
              });
    } finally {
      context.close();
    }
  }
}
