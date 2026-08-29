package com.solnotfound.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  private Integer
      anticipationWindow; // hecho en horas, cantidad de tiempo antes de la actividad para chequear
  private ReprogramationRange reprogramationRange;
  private Boolean availability = false;
  private Boolean weatherChecked = false;
  private User organizer;
  private List<User> participants;

  // las condiciones del clima y avisar a los usuarios

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public User getOrganizer() {
    return organizer;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOrganizer(User organizer) {
    this.organizer = organizer;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<User> getParticipants() {
    return participants;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setParticipants(List<User> participants) {
    this.participants = participants;
  }

  public List<WeatherCondition> getWeatherConditions() {
    return List.copyOf(weatherConditions);
  }

  public void setWeatherConditions(List<WeatherCondition> weatherConditions) {
    this.weatherConditions = List.copyOf(weatherConditions);
  }

  public boolean isTimeToCheckWeatherConditions() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime windowStart = dateTime.minusHours(anticipationWindow);

    boolean withinAnticipationWindow = !windowStart.isAfter(now) && !dateTime.isBefore(now);

    return (withinAnticipationWindow && !weatherChecked);
  }

  public void markWeatherChecked() {
    this.weatherChecked = true;
  }
}
