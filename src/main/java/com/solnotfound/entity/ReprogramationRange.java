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

  public boolean isWithinRange(LocalDateTime originalDateTime, LocalDateTime candidateDateTime) {
    LocalDateTime limit = originalDateTime.plusDays(maxDays);
    LocalTime candidateTime = candidateDateTime.toLocalTime();

    return candidateDateTime.isAfter(originalDateTime)
        && !candidateDateTime.isAfter(limit)
        && !candidateTime.isBefore(initialHour)
        && !candidateTime.isAfter(finalHour);
  }
}
