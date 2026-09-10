package com.solnotfound.entity.notification;

import static org.junit.jupiter.api.Assertions.*;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.user.User;
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
    User receiver = User.withId("user-1");
    Notification notification =
        new Notification(receiver, activity, new BadWeatherAlertNotificationType());
    notification.setId("notif-1");

    // Verify that the title and message are generated (we test the exact strings in the Type test)
    assertNotNull(notification.getTitle());
    assertNotNull(notification.getMessage());

    // Verify initial read flag
    assertFalse(notification.isRead());
    assertSame(receiver, notification.getReceiverUser());
    assertSame(activity, notification.getActivity());
  }

  @Test
  void shouldMarkNotificationAsRead() {
    Notification notification =
        new Notification(
            com.solnotfound.entity.user.User.withId("user-2"),
            activity,
            new StartedNotificationType());
    notification.setId("notif-2");

    assertFalse(notification.isRead());

    notification.setAsRead();

    assertTrue(notification.isRead());
  }
}
