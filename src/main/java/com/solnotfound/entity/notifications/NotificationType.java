package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;
import java.time.format.DateTimeFormatter;

public enum NotificationType {

  BAD_WEATHER_ALERT {
    @Override
    public String generateTitle(Activity activity) {
      String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : "Actividad";
      return "⚠️ Alerta de mal clima: " + activityTitle;
    }

    @Override
    public String generateMessage(Activity activity) {
      String title = activity != null && activity.getTitle() != null ? activity.getTitle() : "tu actividad";
      String dateFormatted = formatDate(activity);
      return "El pronóstico meteorológico para " + title + " programada para el " + dateFormatted + " no cumple con las condiciones esperadas. Se evaluará una reprogramación.";
    }
  },

  STARTED {
    @Override
    public String generateTitle(Activity activity) {
      String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : "Actividad";
      return "🎉 ¡La actividad " + activityTitle + " ha comenzado!";
    }

    @Override
    public String generateMessage(Activity activity) {
      String title = activity != null && activity.getTitle() != null ? activity.getTitle() : "tu actividad";
      return "La actividad " + title + " ya está en curso. ¡Esperamos que la disfrutes!";
    }
  },

  REPROGRAMMED {
    @Override
    public String generateTitle(Activity activity) {
      String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : "Actividad";
      return "📅 Actividad reprogramada: " + activityTitle;
    }

    @Override
    public String generateMessage(Activity activity) {
      String title = activity != null && activity.getTitle() != null ? activity.getTitle() : "tu actividad";
      String dateFormatted = formatDate(activity);
      return "La actividad " + title + " ha sido reprogramada y se llevará a cabo el " + dateFormatted + ".";
    }
  },

  CANCELLED {
    @Override
    public String generateTitle(Activity activity) {
      String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : "Actividad";
      return "❌ Actividad cancelada: " + activityTitle;
    }

    @Override
    public String generateMessage(Activity activity) {
      String title = activity != null && activity.getTitle() != null ? activity.getTitle() : "tu actividad";
      return "Lamentamos informarte que la actividad " + title + " ha sido cancelada.";
    }
  };

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static String formatDate(Activity activity) {
    if (activity == null || activity.getDateTime() == null) {
      return "fecha a confirmar";
    }
    return activity.getDateTime().format(FORMATTER) + " hs";
  }

  public abstract String generateTitle(Activity activity);

  public abstract String generateMessage(Activity activity);
}
//TODO: Revisar y agregar los tipos de notificaciones que sean necesarios.
