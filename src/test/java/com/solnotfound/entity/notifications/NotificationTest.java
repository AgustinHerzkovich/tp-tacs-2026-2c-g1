package com.solnotfound.entity.notifications;

import static org.junit.jupiter.api.Assertions.*;

import com.solnotfound.entity.Activity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    Notification notification =
        new Notification("user-1", activity, new BadWeatherAlertNotificationType());
    notification.setId("notif-1");

    // Verify that the title and message are generated (we test the exact strings in the Type test)
    assertNotNull(notification.getTitle());
    assertNotNull(notification.getMessage());

    // Verify initial read flag
    assertFalse(notification.isRead());
  }

  @Test
  void shouldMarkNotificationAsRead() {
    Notification notification = new Notification("user-2", activity, new StartedNotificationType());
    notification.setId("notif-2");

    assertFalse(notification.isRead());

    notification.setAsRead();

    assertTrue(notification.isRead());
  }
}
