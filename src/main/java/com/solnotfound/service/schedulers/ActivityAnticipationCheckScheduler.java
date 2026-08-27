package com.solnotfound.service.schedulers;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.*;
import com.solnotfound.repository.ActivityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityAnticipationCheckScheduler {

  private final ActivityRepository activityRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;
  private final INotificationFacade notificationFacade;

  @Scheduled(cron = "0 0 * * * *")
  public void checkActivitiesClimate() {

    List<Activity> activities = activityRepository.findAll();

    List<Activity> activitiesToCheck =
        activities.stream().filter(Activity::isTimeToCheckWeatherConditions).toList();

    activitiesToCheck.forEach(
        activity -> {
          Location location = activity.getLocation();
          try {
            WeatherForecast weather =
                weatherAdapter.getFutureClimate(location, activity.getDateTime());
            if (badWeatherChecker.isBadWeatherForActivity(weather, activity)) {
              notificationFacade.notifyBadWeather(activity, weather);
            }
            activity.setWasNotificated(true);
            activityRepository.save(activity);

          } catch (Exception e) {
            log.error(
                "Could not obtain activitie's climate {}: {}", activity.getId(), e.getMessage());
          }
        });
  }
}
