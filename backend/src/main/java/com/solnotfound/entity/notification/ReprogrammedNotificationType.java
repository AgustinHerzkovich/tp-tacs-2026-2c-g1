package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;

public final class ReprogrammedNotificationType implements NotificationType {

  @Override
  public String code() {
    return "REPROGRAMMED";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "📅 Actividad reprogramada: " + ActivityNotificationText.title(activity);
  }

  @Override
  public String generateMessage(Activity activity) {
    return "La actividad "
        + ActivityNotificationText.messageTitle(activity)
        + " ha sido reprogramada y se llevará a cabo el "
        + ActivityNotificationText.date(activity)
        + ".";
  }
}
