package com.solnotfound.listener;

import com.solnotfound.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

  private final NotificationService notificationService;

  public NotificationEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener
  public void handleActivityNotificationEvent(ActivityNotificationEvent event) {
    notificationService.generateNotificationsForActivityEvent(event.activity(), event.type());

    /* Notas sobre handleActivityNotificationEvent:
    - A futuro, si queres que se notifique por otro medio (ej: Telegram, Push, etc.), podes llamar a ese servicio aca mismo. Tendrias que modificar el metodo generateNotificationsForActivityEvent para que devuelva la lista de notificaciones generadas, y luego pasarlas a ese servicio de notificaciones externas.
    - Si queres que sea asincrónico, podes agregar @Async a este metodo.
     */
  }
}
