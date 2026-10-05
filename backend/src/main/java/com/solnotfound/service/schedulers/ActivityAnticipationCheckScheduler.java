package com.solnotfound.service.schedulers;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.notification.BadWeatherAlertNotificationType;
import com.solnotfound.entity.statistics.ActivityTransitionReason;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.IBadWeatherChecker;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.WeatherUnavailableException;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.ActivityStatusTransitionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "app.scheduling.beans-enabled",
    havingValue = "true",
    matchIfMissing = true)
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class ActivityAnticipationCheckScheduler {

  /** Minimum time participants get to vote before the votation closes. */
  private static final Duration MIN_VOTING_TIME = Duration.ofHours(1);

  /**
   * Gap kept between the closing of the votation and its earliest alternative. It covers the hourly
   * closing check, so the activity is rescheduled before the chosen date arrives.
   */
  private static final Duration CLOSING_MARGIN = Duration.ofHours(1);

  private final IActivityRepository activityRepository;
  private final IVotationRepository votationRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;
  private final ApplicationEventPublisher eventPublisher;
  private final ActivityStatusTransitionService transitionService;
  private final Duration votationDuration;
  private final double minQuorum;

  public ActivityAnticipationCheckScheduler(
      IActivityRepository activityRepository,
      IVotationRepository votationRepository,
      IWeatherAdapter weatherAdapter,
      IBadWeatherChecker badWeatherChecker,
      ApplicationEventPublisher eventPublisher,
      ActivityStatusTransitionService transitionService,
      @Value("${votation.duration:24h}") Duration votationDuration,
      @Value("${votation.min-quorum:0.5}") double minQuorum) {
    this.activityRepository = activityRepository;
    this.votationRepository = votationRepository;
    this.weatherAdapter = weatherAdapter;
    this.badWeatherChecker = badWeatherChecker;
    this.eventPublisher = eventPublisher;
    this.transitionService = transitionService;
    this.votationDuration = votationDuration;
    this.minQuorum = minQuorum;
  }

  /**
   * Checks due active activities once per hour. For bad weather, alternatives and the resulting
   * activity state are persisted before notification delivery. Weather-provider failures leave the
   * activity unchecked so a later execution can retry it.
   *
   * <p>Every votation it opens starts with the configured participation quorum ({@code
   * votation.min-quorum}), which the organizer can then adjust from the app.
   */
  @Scheduled(cron = "${activity.weather-check-cron:0 0 * * * *}")
  public void checkActivitiesClimate() {

    List<Activity> activeActivities = activityRepository.findActive();
    int dueActivities = 0;
    int goodWeather = 0;
    int badWeatherActivities = 0;
    int failures = 0;
    log.info("Weather check started: activeActivities={}", activeActivities.size());

    for (Activity activity : activeActivities) {
      if (!activity.isTimeToCheckWeatherConditions()) {
        continue;
      }

      dueActivities++;
      Location location = activity.getLocation();
      try {
        WeatherForecast weather = weatherAdapter.getFutureClimate(location, activity.getDateTime());
        boolean badWeather = badWeatherChecker.isBadWeatherForActivity(weather, activity);
        if (badWeather) {
          badWeatherActivities++;
          openActivityVotation(activity);
          eventPublisher.publishEvent(
              ActivityNotificationEvent.from(activity, new BadWeatherAlertNotificationType()));
        } else {
          goodWeather++;
        }

      } catch (Exception exception) {
        failures++;
        log.error("Could not process weather check: activityId={}", activity.getId(), exception);
      }
    }
    log.info(
        "Weather check completed: activeActivities={} dueActivities={} goodWeather={} badWeather={} failures={}",
        activeActivities.size(),
        dueActivities,
        goodWeather,
        badWeatherActivities,
        failures);
  }

  private void openActivityVotation(Activity activity) {
    if (votationRepository.findActiveByActivityId(activity.getId()) != null) {
      log.info("Weather votation skipped: activityId={} reason=already_active", activity.getId());
      return;
    }

    log.info("Weather votation evaluation started: activityId={}", activity.getId());

    List<LocalDateTime> candidateTimes = new ArrayList<>();

    log.info(
        "Searching for new time options with better weather conditions for activity {}",
        activity.getId());
    for (int i = 1; i <= activity.getReprogramationRange().getMaxDays(); i++) {
      LocalDateTime newTime =
          activity
              .getDateTime()
              .plusDays(i)
              .withHour(activity.getReprogramationRange().getInitialHour().getHour())
              .withMinute(activity.getReprogramationRange().getInitialHour().getMinute())
              .withSecond(0);

      while (activity.getReprogramationRange().isWithinRange(activity.getDateTime(), newTime)) {
        candidateTimes.add(newTime);
        newTime = newTime.plusHours(1);
      }
    }

    // The votation needs time to collect votes and must close before its earliest alternative,
    // so alternatives that start too soon are not offered.
    LocalDateTime creationDate = activity.now();
    LocalDateTime earliestAllowed = creationDate.plus(MIN_VOTING_TIME).plus(CLOSING_MARGIN);
    candidateTimes.removeIf(candidate -> !candidate.isAfter(earliestAllowed));

    List<WeatherForecast> forecasts =
        weatherAdapter.getForecastRange(activity.getLocation(), candidateTimes);
    if (forecasts.size() != candidateTimes.size()) {
      throw new WeatherUnavailableException("Provider returned an incomplete forecast range");
    }
    List<VotationOption> options = new ArrayList<>();
    for (int index = 0; index < candidateTimes.size(); index++) {
      WeatherForecast weather = forecasts.get(index);
      LocalDateTime newTime = candidateTimes.get(index);
      if (!badWeatherChecker.isBadWeatherForActivity(weather, activity)) {
        VotationOption option = new VotationOption();
        option.setDateTime(newTime);
        option.setUsers(new ArrayList<>());
        options.add(option);
      }
    }

    if (options.isEmpty()) {
      transitionService.transition(
          activity, ActivityStatus.CANCELLED, ActivityTransitionReason.NO_WEATHER_ALTERNATIVES);
      log.info(
          "Activity cancelled after weather check: activityId={} reason=no_weather_alternatives",
          activity.getId());
      return;
    }

    Votation votation = new Votation();
    votation.setActivity(activity);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setCreationDate(creationDate);
    votation.setClosingDate(closingDate(creationDate, options.getFirst().getDateTime()));
    votation.setMinQuorum(minQuorum);
    votation.setOptions(options);
    votationRepository.save(votation);
    transitionService.transition(
        activity, ActivityStatus.PROPOSED, ActivityTransitionReason.BAD_WEATHER);
    log.info(
        "Weather votation opened: activityId={} options={} closesAt={}",
        activity.getId(),
        options.size(),
        votation.getClosingDate());
  }

  /**
   * Chooses when the votation closes: after the configured duration, but never later than {@link
   * #CLOSING_MARGIN} before the earliest alternative. Otherwise a short anticipation window could
   * leave the votation open past its own options and reschedule the activity to a past date.
   *
   * @param creationDate when the votation opens, in the activity's zone
   * @param earliestOption earliest alternative offered
   * @return the closing date, always after {@code creationDate}
   */
  private LocalDateTime closingDate(LocalDateTime creationDate, LocalDateTime earliestOption) {
    LocalDateTime byDuration = creationDate.plus(votationDuration);
    LocalDateTime beforeEarliestOption = earliestOption.minus(CLOSING_MARGIN);
    return byDuration.isBefore(beforeEarliestOption) ? byDuration : beforeEarliestOption;
  }
}
