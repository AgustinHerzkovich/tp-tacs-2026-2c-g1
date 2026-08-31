package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;

public final class StartingSoonNotificationType implements NotificationType {

  @Override
  public String code() {
    return "STARTING_SOON";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "La actividad " + ActivityNotificationText.title(activity) + " esta por comenzar";
  }

  @Override
  public String generateMessage(Activity activity) {
    return "La actividad "
        + ActivityNotificationText.messageTitle(activity)
        + " comenzara el "
        + ActivityNotificationText.date(activity)
        + ".";
  }
}
