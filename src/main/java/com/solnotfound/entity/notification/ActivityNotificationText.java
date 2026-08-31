package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;
import java.time.format.DateTimeFormatter;

final class ActivityNotificationText {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private ActivityNotificationText() {}

  static String title(Activity activity) {
    return activity != null && activity.getTitle() != null ? activity.getTitle() : "Actividad";
  }

  static String messageTitle(Activity activity) {
    return activity != null && activity.getTitle() != null ? activity.getTitle() : "tu actividad";
  }

  static String date(Activity activity) {
    if (activity == null || activity.getDateTime() == null) {
      return "fecha a confirmar";
    }
    return activity.getDateTime().format(FORMATTER) + " hs";
  }
}
