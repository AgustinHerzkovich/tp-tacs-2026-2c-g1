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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class ActivityAnticipationCheckScheduler {

  private final IActivityRepository activityRepository;
  private final IVotationRepository votationRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;
  private final ApplicationEventPublisher eventPublisher;
  private final ActivityStatusTransitionService transitionService;
  private final Duration votationDuration;

  public ActivityAnticipationCheckScheduler(
      IActivityRepository activityRepository,
      IVotationRepository votationRepository,
      IWeatherAdapter weatherAdapter,
      IBadWeatherChecker badWeatherChecker,
      ApplicationEventPublisher eventPublisher,
      ActivityStatusTransitionService transitionService,
      @Value("${votation.duration:24h}") Duration votationDuration) {
    this.activityRepository = activityRepository;
    this.votationRepository = votationRepository;
    this.weatherAdapter = weatherAdapter;
    this.badWeatherChecker = badWeatherChecker;
    this.eventPublisher = eventPublisher;
    this.transitionService = transitionService;
    this.votationDuration = votationDuration;
  }

  /**
   * Checks due active activities once per hour. For bad weather, alternatives and the resulting
   * activity state are persisted before notification delivery. Weather-provider failures leave the
   * activity unchecked so a later execution can retry it.
   */
  @Scheduled(cron = "0 0 * * * *")
  public void checkActivitiesClimate() {

    List<Activity> activeActivities = activityRepository.findActive();

    activeActivities.forEach(
        activity -> {
          if (!activity.isTimeToCheckWeatherConditions()) {
            return;
          }

          Location location = activity.getLocation();
          try {
            WeatherForecast weather =
                weatherAdapter.getFutureClimate(location, activity.getDateTime());
            boolean badWeather = badWeatherChecker.isBadWeatherForActivity(weather, activity);
            if (badWeather) {
              openActivityVotation(activity);
              eventPublisher.publishEvent(
                  ActivityNotificationEvent.from(activity, new BadWeatherAlertNotificationType()));
            } else {
              activity.markWeatherChecked();
              activityRepository.save(activity);
            }

          } catch (Exception e) {
            log.error(
                "Could not obtain activitie's climate {}: {}", activity.getId(), e.getMessage());
          }
        });
  }

  private void openActivityVotation(Activity activity) {
    if (votationRepository.findActiveByActivityId(activity.getId()) != null) {
      log.info("Activity {} has an active votation active already", activity.getId());
      return;
    }

    log.info("Opening new active votation for activity {}", activity.getId());

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

    activity.markWeatherChecked();
    if (options.isEmpty()) {
      transitionService.transition(
          activity, ActivityStatus.CANCELLED, ActivityTransitionReason.NO_WEATHER_ALTERNATIVES);
      return;
    }

    Votation votation = new Votation();
    votation.setActivity(activity);
    votation.setStatus(VotationStatus.ACTIVE);
    LocalDateTime creationDate = LocalDateTime.now();
    votation.setCreationDate(creationDate);
    votation.setClosingDate(creationDate.plus(votationDuration));
    votation.setOptions(options);
    votationRepository.save(votation);
    transitionService.transition(
        activity, ActivityStatus.PROPOSED, ActivityTransitionReason.BAD_WEATHER);
  }
}
