package com.solnotfound.service.schedulers;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.*;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.repository.VotationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
  private final VotationRepository votationRepository;
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
              openActivityVotation(activity);
            }
            activity.markWeatherChecked();

          } catch (Exception e) {
            // no se marca la actividad como revisada, para que se vuelva a intentar en el siguiente
            // ciclo dentro de 1 hora
            log.error(
                "Could not obtain activitie's climate {}: {}", activity.getId(), e.getMessage());
          }
        });
  }

  private void openActivityVotation(Activity activity) {
    if (votationRepository.findActiveVotationByActivityId(activity.getId()) != null) {
      log.info("Activity {} has an active votation active already", activity.getId());
      return;
    }

    log.info("Opening new active votation for activity {}", activity.getId());

    Votation votation = new Votation();
    votation.setActivity(activity);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setCreationDate(LocalDateTime.now());

    List<VotationOption> options = new ArrayList<>();
    votation.setOptions(options);

    log.info(
        "Searching for new time options with better weather conditions for activity {}",
        activity.getId());
    for (int i = 1; i <= activity.getReprogramationRange().getMaxDays(); i++) {
      LocalDateTime newTime =
          activity
              .getDateTime()
              .plusDays(i)
              .withHour(activity.getReprogramationRange().getInitialHour().getHour())
              .withMinute(0)
              .withSecond(0);

      while (activity.getReprogramationRange().isWithinRange(activity.getDateTime(), newTime)) {
        WeatherForecast weather = weatherAdapter.getFutureClimate(activity.getLocation(), newTime);
        if (!badWeatherChecker.isBadWeatherForActivity(weather, activity)) {
          VotationOption option = new VotationOption();
          option.setDateTime(newTime);
          option.setUsers(new ArrayList<>());
          options.add(option);
        }
        newTime = newTime.plusHours(1);
      }
    }

    votationRepository.save(votation);
  }
}
