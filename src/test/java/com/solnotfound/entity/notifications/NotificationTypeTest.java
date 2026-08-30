package com.solnotfound.entity.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solnotfound.entity.Activity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationTypeTest {

  private Activity activity;

  @BeforeEach
  void setUp() {
    activity = new Activity();
    activity.setId("act-123");
    activity.setTitle("Partido de Fútbol");
    activity.setDateTime(LocalDateTime.of(2026, 10, 15, 18, 30));
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForBadWeather() {
    NotificationType type = new BadWeatherAlertNotificationType();
    String title = type.generateTitle(activity);
    String message = type.generateMessage(activity);

    assertEquals("⚠️ Alerta de mal clima: Partido de Fútbol", title);
    assertEquals(
        "El pronóstico meteorológico para Partido de Fútbol programada para el 15/10/2026 18:30 hs no cumple con las condiciones esperadas. Se evaluará una reprogramación.",
        message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForStarted() {
    NotificationType type = new StartedNotificationType();
    String title = type.generateTitle(activity);
    String message = type.generateMessage(activity);

    assertEquals("🎉 ¡La actividad Partido de Fútbol ha comenzado!", title);
    assertEquals(
        "La actividad Partido de Fútbol ya está en curso. ¡Esperamos que la disfrutes!", message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForReprogrammed() {
    NotificationType type = new ReprogrammedNotificationType();
    String title = type.generateTitle(activity);
    String message = type.generateMessage(activity);

    assertEquals("📅 Actividad reprogramada: Partido de Fútbol", title);
    assertEquals(
        "La actividad Partido de Fútbol ha sido reprogramada y se llevará a cabo el 15/10/2026 18:30 hs.",
        message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForCancelled() {
    NotificationType type = new CancelledNotificationType();
    String title = type.generateTitle(activity);
    String message = type.generateMessage(activity);

    assertEquals("❌ Actividad cancelada: Partido de Fútbol", title);
    assertEquals(
        "Lamentamos informarte que la actividad Partido de Fútbol ha sido cancelada.", message);
  }

  @Test
  void shouldHandleNullActivityTitleGracefully() {
    Activity emptyActivity = new Activity();
    emptyActivity.setDateTime(LocalDateTime.of(2026, 11, 20, 10, 0));

    NotificationType type = new CancelledNotificationType();
    String title = type.generateTitle(emptyActivity);
    String message = type.generateMessage(emptyActivity);

    assertEquals("❌ Actividad cancelada: Actividad", title);
    assertEquals("Lamentamos informarte que la actividad tu actividad ha sido cancelada.", message);
  }
}
