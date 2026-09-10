package com.solnotfound.entity.activity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum ActivityStatus {
  /*
  Además del estado tener en cuenta que puede o no estar habilitado
  Confirmed: La actividad inicia en este estado y se suman los miembros.
  Proposed: Cuando ocurre una reprogramacion primero entra en estado de votacion que es este.
  Rescheduled: Una vez elegida la opcion pasa a este estado
  Cancelled: Se cancelo la actividad
  Finished: La actividad se concreto con exito pero esta en pasado
   */
  CONFIRMED,
  PROPOSED,
  RESCHEDULED,
  CANCELLED,
  FINISHED;

  @JsonCreator
  public static ActivityStatus fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return ActivityStatus.valueOf(value.toUpperCase(Locale.ROOT));
  }
}
