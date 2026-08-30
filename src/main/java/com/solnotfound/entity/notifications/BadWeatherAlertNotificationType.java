package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;

public final class BadWeatherAlertNotificationType implements NotificationType {

  @Override
  public String code() {
    return "BAD_WEATHER_ALERT";
  }

  @Override
  public String generateTitle(Activity activity) {
    return "⚠️ Alerta de mal clima: " + ActivityNotificationText.title(activity);
  }

  @Override
  public String generateMessage(Activity activity) {
    return "El pronóstico meteorológico para "
        + ActivityNotificationText.messageTitle(activity)
        + " programada para el "
        + ActivityNotificationText.date(activity)
        + " no cumple con las condiciones esperadas. Se evaluará una reprogramación.";
  }
}
