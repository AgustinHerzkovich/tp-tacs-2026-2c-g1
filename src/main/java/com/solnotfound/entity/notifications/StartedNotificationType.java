package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;

public final class StartedNotificationType implements NotificationType {

  @Override
  public String code() {
    return "STARTED";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "🎉 ¡La actividad " + ActivityNotificationText.title(activity) + " ha comenzado!";
  }

  @Override
  public String generateMessage(Activity activity) {
    return "La actividad "
        + ActivityNotificationText.messageTitle(activity)
        + " ya está en curso. ¡Esperamos que la disfrutes!";
  }
}
