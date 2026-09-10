package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;

public final class CancelledNotificationType implements NotificationType {

  @Override
  public String code() {
    return "CANCELLED";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "❌ Actividad cancelada: " + ActivityNotificationText.title(activity);
  }

  @Override
  public String generateMessage(Activity activity) {
    return "Lamentamos informarte que la actividad "
        + ActivityNotificationText.messageTitle(activity)
        + " ha sido cancelada.";
  }
}
