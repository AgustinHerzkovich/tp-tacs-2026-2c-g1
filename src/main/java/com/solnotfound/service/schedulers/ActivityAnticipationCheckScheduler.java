package com.solnotfound.service.schedulers;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.entity.notifications.BadWeatherAlertNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class ActivityAnticipationCheckScheduler {

  private final IActivityRepository activityRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Checks due active activities once per hour. Successful checks are persisted before publishing
   * bad-weather events, preventing notification failures from causing duplicate weather checks.
   * Weather-provider failures leave the activity unchecked so a later execution can retry it.
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
            activity.markWeatherChecked();
            activityRepository.save(activity);
            if (badWeather) {
              eventPublisher.publishEvent(
                  ActivityNotificationEvent.from(activity, new BadWeatherAlertNotificationType()));
            }

          } catch (Exception e) {
            log.error(
                "Could not obtain activitie's climate {}: {}", activity.getId(), e.getMessage());
          }
        });
  }
}
