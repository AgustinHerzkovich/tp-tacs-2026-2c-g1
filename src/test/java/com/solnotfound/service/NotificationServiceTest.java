package com.solnotfound.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.notifications.BadWeatherAlertNotificationType;
import com.solnotfound.entity.notifications.Notification;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.INotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private INotificationRepository notificationRepository;

  @InjectMocks private NotificationService notificationService;

  private Notification notification;
  private Activity activity;

  @BeforeEach
  void setUp() {
    activity = new Activity();
    activity.setId("act-1");

    notification =
        new Notification("user-auth-123", activity, new BadWeatherAlertNotificationType());
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

    assertThrows(
        ResourceNotFoundException.class,
        () -> notificationService.markAsRead("notif-invalid", "user-auth-123"));

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void markAsRead_ThrowsException_WhenUserIsNotTheOwner() {
    when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

    assertThrows(
        AccessDeniedException.class,
        () -> notificationService.markAsRead("notif-1", "other-user-456"));

    assertFalse(notification.isRead());
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void generatesNotificationsForCreatorAndParticipantsWithoutDuplicates() {
    activity.setCreatorUserId("creator-1");
    activity.setMaxParticipants(3);
    activity.setMinParticipants(1);
    activity.addParticipant("creator-1");
    activity.addParticipant("participant-1");

    notificationService.generateNotificationsForActivityEvent(
        activity, new BadWeatherAlertNotificationType());

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<Iterable<Notification>> captor =
        org.mockito.ArgumentCaptor.forClass(Iterable.class);
    verify(notificationRepository).saveAll(captor.capture());
    List<String> receivers =
        java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false)
            .map(Notification::getReceiverUser)
            .toList();
    assertEquals(List.of("creator-1", "participant-1"), receivers);
  }
}
