package com.solnotfound.service;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.notifications.Notification;
import com.solnotfound.entity.notifications.NotificationType;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.INotificationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private INotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  private Notification notification;

  @BeforeEach
  void setUp() {
    Activity activity = new Activity();
    activity.setId("act-1");

    notification = new Notification("user-auth-123", activity, NotificationType.BAD_WEATHER_ALERT);
    notification.setId("notif-1");
  }

  @Test
  void markAsRead_Success() {
    when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

    notificationService.markAsRead("notif-1", "user-auth-123");

    assertTrue(notification.isRead());
    verify(notificationRepository, times(1)).save(notification);
  }

  @Test
  void markAsRead_ThrowsException_WhenNotificationNotFound() {
    when(notificationRepository.findById("notif-invalid")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead("notif-invalid", "user-auth-123"));

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void markAsRead_ThrowsException_WhenUserIsNotTheOwner() {
    when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

    assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead("notif-1", "other-user-456"));

    assertFalse(notification.isRead());
    verify(notificationRepository, never()).save(any());
  }
}
