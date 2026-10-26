package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;

/**
 * Notification published when the organizer replaces the alternative dates of an open votation, so
 * the participants find out that the list they are voting on changed (including when one of their
 * own votes was dropped) instead of discovering it silently in the app.
 */
public final class VotationOptionsChangedNotificationType implements NotificationType {

  @Override
  public String code() {
    return "VOTATION_OPTIONS_CHANGED";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "🗳️ Cambiaron las fechas de la votación: " + ActivityNotificationText.title(activity);
  }

  @Override
  public String generateMessage(Activity activity) {
    return "El organizador de "
        + ActivityNotificationText.messageTitle(activity)
        + " cambió las fechas alternativas de la votación. Revisá las opciones y votá de nuevo si"
        + " querés cambiar tu voto.";
  }
}
