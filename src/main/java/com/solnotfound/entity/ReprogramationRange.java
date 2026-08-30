package com.solnotfound.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Rango de reprogramación permitido por el organizador: hasta cuántos días después de la fecha
 * original se puede reprogramar una actividad, y en qué franja horaria del día es aceptable.
 */
@Getter
@AllArgsConstructor
public class ReprogramationRange {
  private final Integer maxDays;
  private final LocalTime initialHour;
  private final LocalTime finalHour;

  /**
   * Checks whether a candidate date is after the original date, no later than the configured day
   * limit, and inside the allowed daily time window.
   *
   * @param originalDateTime original activity date and time
   * @param candidateDateTime proposed replacement date and time
   * @return {@code true} when the candidate satisfies every range constraint
   */
  public boolean isWithinRange(LocalDateTime originalDateTime, LocalDateTime candidateDateTime) {
    LocalDateTime limit = originalDateTime.toLocalDate().plusDays(maxDays).atTime(LocalTime.MAX);
    LocalTime candidateTime = candidateDateTime.toLocalTime();

    return candidateDateTime.isAfter(originalDateTime)
        && !candidateDateTime.isAfter(limit)
        && !candidateTime.isBefore(initialHour)
        && !candidateTime.isAfter(finalHour);
  }
}
