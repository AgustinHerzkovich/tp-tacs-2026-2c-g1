package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    String title = NotificationType.BAD_WEATHER_ALERT.generateTitle(activity);
    String message = NotificationType.BAD_WEATHER_ALERT.generateMessage(activity);

    assertEquals("⚠️ Alerta de mal clima: Partido de Fútbol", title);
    assertEquals("El pronóstico meteorológico para Partido de Fútbol programada para el 15/10/2026 18:30 hs no cumple con las condiciones esperadas. Se evaluará una reprogramación.", message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForStarted() {
    String title = NotificationType.STARTED.generateTitle(activity);
    String message = NotificationType.STARTED.generateMessage(activity);

    assertEquals("🎉 ¡La actividad Partido de Fútbol ha comenzado!", title);
    assertEquals("La actividad Partido de Fútbol ya está en curso. ¡Esperamos que la disfrutes!", message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForReprogrammed() {
    String title = NotificationType.REPROGRAMMED.generateTitle(activity);
    String message = NotificationType.REPROGRAMMED.generateMessage(activity);

    assertEquals("📅 Actividad reprogramada: Partido de Fútbol", title);
    assertEquals("La actividad Partido de Fútbol ha sido reprogramada y se llevará a cabo el 15/10/2026 18:30 hs.", message);
  }

  @Test
  void shouldGenerateCorrectTitleAndMessageForCancelled() {
    String title = NotificationType.CANCELLED.generateTitle(activity);
    String message = NotificationType.CANCELLED.generateMessage(activity);

    assertEquals("❌ Actividad cancelada: Partido de Fútbol", title);
    assertEquals("Lamentamos informarte que la actividad Partido de Fútbol ha sido cancelada.", message);
  }

  @Test
  void shouldHandleNullActivityTitleGracefully() {
    Activity emptyActivity = new Activity();
    emptyActivity.setDateTime(LocalDateTime.of(2026, 11, 20, 10, 0));

    String title = NotificationType.CANCELLED.generateTitle(emptyActivity);
    String message = NotificationType.CANCELLED.generateMessage(emptyActivity);

    assertEquals("❌ Actividad cancelada: Actividad", title);
    assertEquals("Lamentamos informarte que la actividad tu actividad ha sido cancelada.", message);
  }
}
