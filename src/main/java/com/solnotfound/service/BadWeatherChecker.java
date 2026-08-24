package com.solnotfound.service;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.WeatherForecast;
import org.springframework.stereotype.Component;

/**
 * Determina si un pronóstico incumple alguna de las condiciones de clima definidas por el
 * organizador para la actividad.
 */
@Component
public class BadWeatherChecker implements IBadWeatherChecker {

  @Override
  public boolean isBadWeatherForActivity(WeatherForecast weather, Activity activity) {
    return activity.getWeatherConditions().stream()
        .anyMatch(condition -> !condition.isSatisfiedBy(weather));
  }
}
