package com.solnotfound.service;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.notification.Notification;
import com.solnotfound.entity.notification.NotificationType;
import com.solnotfound.entity.user.User;
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

  /**
   * Returns unread notifications for a user, ordered according to the repository contract.
   *
   * @param currentUserId authenticated receiver identifier
   * @return unread notifications belonging to the user
   */
  public List<NotificationResponse> getNotificationsByUser(String currentUserId) {
    return notificationRepository.findByReadAndReceiverUserId(false, currentUserId).stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Generates and persists one notification for the creator and each participant. The creator is
   * not duplicated when also present in the participant list.
   *
   * @param activity source activity
   * @param type strategy used to identify and render each notification
   */
  public void generateNotificationsForActivityEvent(Activity activity, NotificationType type) {
    List<Notification> notificationsToSave = new ArrayList<>();

    User creator = activity.getOrganizer();
    String creatorId = creator.getId();
    notificationsToSave.add(new Notification(creator, activity, type));

    for (User participant : activity.getParticipants()) {
      if (!participant.getId().equals(creatorId)) {
        notificationsToSave.add(new Notification(participant, activity, type));
      }
    }

    notificationRepository.saveAll(notificationsToSave);
  }

  /**
   * Marks a notification as read only when it belongs to the authenticated user.
   *
   * @param notificationId notification identifier
   * @param currentUserId authenticated user identifier
   * @throws ResourceNotFoundException when the notification does not exist
   * @throws AccessDeniedException when the notification belongs to another user
   */
  public void markAsRead(String notificationId, String currentUserId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No se encontró la notificación con el ID: " + notificationId));

    if (!notification.getReceiverUser().getId().equals(currentUserId)) {
      throw new AccessDeniedException("La notificación no pertenece al usuario actual.");
    }

    notification.setAsRead();
    notificationRepository.save(notification);
  }

  private NotificationResponse toDto(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getActivity().getId(),
        notification.getType().code(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getCreatedAt());
  }
}
