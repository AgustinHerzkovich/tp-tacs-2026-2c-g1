package com.solnotfound.service;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.entity.notifications.Notification;
import com.solnotfound.repository.INotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final INotificationRepository notificationRepository;

  public NotificationService(INotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public List<NotificationResponse> getNotificationsByUser(String userId) {
    return notificationRepository.findByReadAndReceiverUserId(false, userId).stream()
      .map(this::toDto)
      .toList();
  }

  public void markAsRead(String notificationId) {

    // TODO: Handle the case where the notification is not found (throw an exception)

    notificationRepository.findById(notificationId).ifPresent(notification -> {
      notification.setAsRead();
      notificationRepository.save(notification);
    });
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
