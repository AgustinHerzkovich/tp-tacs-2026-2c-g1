package com.solnotfound.service;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.Participant;
import com.solnotfound.entity.notifications.Notification;
import com.solnotfound.entity.notifications.NotificationType;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.INotificationRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final INotificationRepository notificationRepository;

  public NotificationService(INotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public List<NotificationResponse> getNotificationsByUser(String currentUserId) {
    return notificationRepository.findByReadAndReceiverUserId(false, currentUserId).stream()
      .map(this::toDto)
      .toList();
  }

  /**
   * Genera y persiste notificaciones para el organizador y los participantes de una actividad.
   */
  public void generateNotificationsForActivityEvent(Activity activity, NotificationType type) {
    List<Notification> notificationsToSave = new ArrayList<>();

    // Notification for the owner/creator of the activity
    String creatorId = activity.getCreator() != null ? activity.getCreator() : "system"; //TODO: Cambiar por el usuario real (tras merge)
    notificationsToSave.add(
      new Notification(creatorId, activity, type)
    );

    //Notification for the participants of the activity (Not include the creator)
    List<String> participantIds = activity.getParticipants().stream().map(Participant::getUserId).toList();
    //TODO: Cambiar por los usuarios reales (tras merge)

    for (String participantId : participantIds) {
      if (!participantId.equals(creatorId)) {
        notificationsToSave.add(
          new Notification(participantId, activity, type)
        );
      }
    }

    notificationRepository.saveAll(notificationsToSave);
  }

  public void markAsRead(String notificationId, String currentUserId) {
    Notification notification = notificationRepository.findById(notificationId)
      .orElseThrow(() -> new ResourceNotFoundException("No se encontró la notificación con el ID: " + notificationId));


    //TODO: Cuando tengamos el User real, tomar de este su id para la comparacion
    if (!notification.getReceiverUser().equals(currentUserId)) {
      throw new AccessDeniedException("La notificación no pertenece al usuario actual.");
    }

    notification.setAsRead();
    notificationRepository.save(notification);
  }

  private NotificationResponse toDto(Notification notification) {
    return new NotificationResponse(
      notification.getId(),
      notification.getActivity().getId(),
      notification.getType(),
      notification.getTitle(),
      notification.getMessage(),
      notification.getCreatedAt()
    );
  }
}
