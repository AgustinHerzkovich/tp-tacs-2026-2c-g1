package com.solnotfound.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Activity {
  private String id;
  private String title;
  private String description;
  private ActivityType type;
  private Location location;
  private LocalDateTime dateTime;
  private Integer minParticipants;
  private Integer maxParticipants;
  private List<WeatherCondition> weatherConditions = List.of();

  public List<WeatherCondition> getWeatherConditions() {
    return List.copyOf(weatherConditions);
  }

  public void setWeatherConditions(List<WeatherCondition> weatherConditions) {
    this.weatherConditions = List.copyOf(weatherConditions);
  }
}
