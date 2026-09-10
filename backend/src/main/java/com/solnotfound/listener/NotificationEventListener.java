package com.solnotfound.listener;

import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

  private final NotificationService notificationService;
  private final IActivityRepository activityRepository;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared in-memory repository")
  public NotificationEventListener(
      NotificationService notificationService, IActivityRepository activityRepository) {
    this.notificationService = notificationService;
    this.activityRepository = activityRepository;
  }

  @EventListener
  public void handleActivityNotificationEvent(ActivityNotificationEvent event) {
    notificationService.generateNotificationsForActivityEvent(
        activityRepository.findById(event.activityId()), event.type());

    /* Notas sobre handleActivityNotificationEvent:
    - A futuro, si queres que se notifique por otro medio (ej: Telegram, Push, etc.), podes llamar a ese servicio aca mismo. Tendrias que modificar el metodo generateNotificationsForActivityEvent para que devuelva la lista de notificaciones generadas, y luego pasarlas a ese servicio de notificaciones externas.
    - Si queres que sea asincrónico, podes agregar @Async a este metodo.
     */
  }
}
