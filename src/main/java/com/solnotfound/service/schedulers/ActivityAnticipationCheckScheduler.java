package com.solnotfound.service.schedulers;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.*;
import com.solnotfound.entity.notifications.NotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.ActivityRepository;
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

  private final ActivityRepository activityRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;
  private final ApplicationEventPublisher eventPublisher;

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
            if (badWeatherChecker.isBadWeatherForActivity(weather, activity)) {
              eventPublisher.publishEvent(new ActivityNotificationEvent(activity, NotificationType.BAD_WEATHER_ALERT));
            }
            activity.markWeatherChecked();
            activityRepository.save(activity);

      } catch (Exception e) {
        // no se marca la actividad como revisada, para que se vuelva a intentar en el siguiente ciclo, dentro de 1 hora
        log.error("Could not obtain activitie's climate {}: {}", activity.getId(), e.getMessage());
      }
    });
  }
}
