package com.solnotfound.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.repository.InMemoryActivityRepository;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LegacyActivityTimeZoneBackfillTest {

  private final InMemoryActivityRepository repository = new InMemoryActivityRepository();

  @Test
  void assignsTheConfiguredZoneOnlyToActivitiesWithoutOne() {
    Activity legacy = activity("legacy", null);
    Activity current = activity("current", "Europe/Madrid");

    new LegacyActivityTimeZoneBackfill(repository, " America/Argentina/Buenos_Aires ").run();

    assertThat(legacy.getTimeZone()).isEqualTo("America/Argentina/Buenos_Aires");
    assertThat(current.getTimeZone()).isEqualTo("Europe/Madrid");
  }

  @Test
  void doesNothingWhenNoZoneIsConfigured() {
    Activity legacy = activity("legacy", null);

    new LegacyActivityTimeZoneBackfill(repository, "").run();

    assertThat(legacy.getTimeZone()).isNull();
  }

  @Test
  void rejectsAnUnknownZoneInsteadOfStoringIt() {
    Activity legacy = activity("legacy", null);
    LegacyActivityTimeZoneBackfill backfill =
        new LegacyActivityTimeZoneBackfill(repository, "Not/AZone");

    assertThatThrownBy(backfill::run).isInstanceOf(DateTimeException.class);
    assertThat(legacy.getTimeZone()).isNull();
  }

  private Activity activity(String id, String timeZone) {
    Activity activity = new Activity();
    activity.setId(id);
    activity.setDateTime(LocalDateTime.of(2026, 10, 10, 18, 0));
    activity.setTimeZone(timeZone);
    repository.save(activity);
    return activity;
  }
}
