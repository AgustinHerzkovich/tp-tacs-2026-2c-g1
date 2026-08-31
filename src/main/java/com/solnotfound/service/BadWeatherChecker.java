package com.solnotfound.service;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.weather.IBadWeatherChecker;
import com.solnotfound.entity.weather.WeatherForecast;
import org.springframework.stereotype.Component;

/**
 * Determina si un pronóstico incumple alguna de las condiciones de clima definidas por el
 * organizador para la actividad.
 */
@Component
public class BadWeatherChecker implements IBadWeatherChecker {

  /**
   * Evaluates every configured condition and reports bad weather when at least one is violated.
   *
   * @param weather forecast to evaluate
   * @param activity activity whose accepted conditions are used
   * @return {@code true} when any condition rejects the forecast
   */
  @Override
  public boolean isBadWeatherForActivity(WeatherForecast weather, Activity activity) {
    return activity.getWeatherConditions().stream()
        .anyMatch(condition -> !condition.isSatisfiedBy(weather));
  }
}
