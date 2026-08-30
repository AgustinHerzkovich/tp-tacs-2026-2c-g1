package com.solnotfound.service.schedulers;

import com.solnotfound.entity.Activity;
import com.solnotfound.repository.ActivityRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects the shared in-memory repository")
public class ActivityStatusScheduler {
  private final ActivityRepository activityRepository;

  @Scheduled(cron = "0 0 * * * *")
  public void finishPastActivities() {
    LocalDateTime now = LocalDateTime.now();

    for (Activity activity : activityRepository.findActive()) {
      if (activity.finishIfPast(now)) {
        activityRepository.save(activity);
      }
    }
  }
}
