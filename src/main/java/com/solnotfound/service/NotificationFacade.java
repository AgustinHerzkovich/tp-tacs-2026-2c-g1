package com.solnotfound.service;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.INotificationFacade;
import com.solnotfound.entity.WeatherForecast;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Notifica a los participantes de una actividad cuando el pronóstico deja de cumplir las
 * condiciones de clima definidas. Placeholder por log: reemplazar por el canal real (frontend,
 * email, Telegram, etc.) cuando esté definido.
 */
@Slf4j
@Component
public class NotificationFacade implements INotificationFacade {

  @Override
  public void notifyBadWeather(Activity activity, WeatherForecast weather) {
    log.info(
        "Bad weather detected for activity {} ({}): notifying participants",
        activity.getId(),
        activity.getTitle());
  }
}
