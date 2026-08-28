package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

  private Activity activity;

  @BeforeEach
  void setUp() {
    activity = new Activity();
    activity.setId("act-123");
    activity.setTitle("Partido de Fútbol");
    activity.setDateTime(LocalDateTime.of(2026, 10, 15, 18, 30));
  }

  @Test
  void shouldInitializeWithCorrectStateAndAssociations() {
    Notification notification = new Notification("user-1", activity, NotificationType.BAD_WEATHER_ALERT);
    notification.setId("notif-1");

    // Verify that the title and message are generated (we test the exact strings in the Type test)
    assertNotNull(notification.getTitle());
    assertNotNull(notification.getMessage());

    // Verify initial status and read flag
    assertEquals(NotificationStatus.PENDING, notification.getStatus());
    assertFalse(notification.isRead());
  }

  @Test
  void shouldMarkNotificationAsRead() {
    Notification notification = new Notification("user-2", activity, NotificationType.STARTED);
    notification.setId("notif-2");

    assertFalse(notification.isRead());

    notification.setAsRead();

    assertTrue(notification.isRead());
  }
}
