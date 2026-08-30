package com.solnotfound.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ReprogramationRangeTest {

  private final ReprogramationRange range =
      new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(20, 0));
  private final LocalDateTime original = LocalDateTime.of(2026, 1, 1, 18, 0);

  @Test
  void acceptsACandidateWithinDaysAndHourWindow() {
    LocalDateTime candidate = original.plusDays(1).withHour(15).withMinute(0);

    assertThat(range.isWithinRange(original, candidate)).isTrue();
  }

  @Test
  void rejectsACandidateBeforeTheOriginalDateTime() {
    LocalDateTime candidate = original.minusHours(1);

    assertThat(range.isWithinRange(original, candidate)).isFalse();
  }

  @Test
  void rejectsACandidateBeyondMaxDays() {
    LocalDateTime candidate = original.plusDays(4).withHour(15).withMinute(0);

    assertThat(range.isWithinRange(original, candidate)).isFalse();
  }

  @Test
  void rejectsACandidateOutsideTheHourWindow() {
    LocalDateTime candidate = original.plusDays(1).withHour(9).withMinute(0);

    assertThat(range.isWithinRange(original, candidate)).isFalse();
  }

  @Test
  void acceptsACandidateExactlyAtTheHourWindowBoundaries() {
    LocalDateTime atStart = original.plusDays(1).withHour(10).withMinute(0);
    LocalDateTime atEnd = original.plusDays(1).withHour(20).withMinute(0);

    assertThat(range.isWithinRange(original, atStart)).isTrue();
    assertThat(range.isWithinRange(original, atEnd)).isTrue();
  }

  @Test
  void acceptsCandidateAfterOriginalTimeOnLastAllowedDay() {
    LocalDateTime candidate = original.plusDays(3).withHour(20).withMinute(0);

    assertThat(range.isWithinRange(original, candidate)).isTrue();
  }
}
