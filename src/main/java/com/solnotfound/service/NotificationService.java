package com.solnotfound.service;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.entity.notifications.Notification;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.INotificationRepository;
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

  public void markAsRead(String notificationId, String currentUserId) {
    Notification notification = notificationRepository.findById(notificationId)
      .orElseThrow(() -> new ResourceNotFoundException("No se encontró la notificación con el ID: " + notificationId));


    //TODO: Cuando tengamos el User real, tomar de este su id para la comparacion
    if (!notification.getReceiverUser().equals(currentUserId)) {
      throw new AccessDeniedException("La notificación no pertenece al usuario autenticado");
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
