package com.solnotfound.seed;

import com.solnotfound.repository.IActivityRepository;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Assigns a time zone to activities created before the zone was stored.
 *
 * <p>Deadlines are evaluated in each activity's zone. An activity without one falls back to the JVM
 * zone (UTC in containers), which shifts its deadlines by the organizer's offset. Changing the JVM
 * zone is not an option: Spring Data converts stored dates with it, so every saved date would be
 * read back shifted. This step runs on startup, is idempotent and does nothing when {@code
 * activity.legacy-time-zone} is blank.
 */
@Slf4j
@Component
public class LegacyActivityTimeZoneBackfill implements CommandLineRunner {

  private final IActivityRepository activityRepository;
  private final String legacyTimeZone;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared activity repository")
  public LegacyActivityTimeZoneBackfill(
      IActivityRepository activityRepository,
      @Value("${activity.legacy-time-zone:}") String legacyTimeZone) {
    this.activityRepository = activityRepository;
    this.legacyTimeZone = legacyTimeZone;
  }

  @Override
  public void run(String... args) {
    if (legacyTimeZone == null || legacyTimeZone.isBlank()) {
      return;
    }
    // Fails fast on a typo instead of storing a zone that no activity could later resolve.
    String zone = ZoneId.of(legacyTimeZone.trim()).getId();
    long updated = activityRepository.assignTimeZoneWhereMissing(zone);
    log.info("Legacy activity time zone backfill: zone={} updated={}", zone, updated);
  }
}
