package com.solnotfound.entity;

import com.solnotfound.exception.IllegalStateActivityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class Activity {

  @Getter @Setter private String id;
  @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private ActivityType type;
  @Getter @Setter private Location location;
  @Getter @Setter private LocalDateTime dateTime;
  @Getter @Setter private Integer minParticipants;
  @Getter @Setter private Integer maxParticipants;
  private List<Participant> participants = new ArrayList<>();
  private List<WeatherCondition> weatherConditions = List.of();

  @Setter @Getter
  private Integer
      anticipationWindow; // hecho en horas, cantidad de tiempo antes de la actividad para chequear

  @Setter @Getter private ReprogramationRange reprogramationRange;
  @Setter @Getter private Boolean availability = false;
  @Getter private ActivityStatus status = ActivityStatus.CONFIRMED;
  private List<ActivityStatus> statusHistory = new ArrayList<>(List.of(ActivityStatus.CONFIRMED));
  @Setter @Getter private Boolean weatherChecked = false;

  // las condiciones del clima y avisar a los usuarios

  public List<WeatherCondition> getWeatherConditions() {
    return List.copyOf(weatherConditions);
  }

  public void setWeatherConditions(List<WeatherCondition> weatherConditions) {
    this.weatherConditions = List.copyOf(weatherConditions);
  }

  public List<Participant> getParticipants() {
    return List.copyOf(participants);
  }

  public List<ActivityStatus> getStatusHistory() {
    return List.copyOf(statusHistory);
  }

  public void setStatus(ActivityStatus newStatus) {
    // TODO: Validar transiciones permitidas entre estados.
    status = newStatus;
    statusHistory.add(newStatus);
  }

  public void addParticipant(String userId) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be added to an activity in that status.");
    }

    boolean isFull = participants.size() >= maxParticipants;

    if (isFull) {
      throw new IllegalStateActivityException("Activity has no available spots.");
    }

    boolean alreadyParticipating =
        participants.stream().anyMatch(participant -> participant.getUserId().equals(userId));

    if (alreadyParticipating) {
      throw new IllegalStateActivityException("User is already participating in this activity.");
    }

    participants.add(new Participant(userId));

    if (participants.size() >= minParticipants) {
      availability = true;
    }
  }

  public void removeParticipant(String userId) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be removed to an activity in that status.");
    }

    boolean removed = participants.removeIf(participant -> participant.getUserId().equals(userId));

    if (!removed) {
      throw new IllegalStateActivityException("User is not participating in this activity");
    }

    if (participants.size() < minParticipants) {
      availability = false;
    }
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

  private boolean cannotChangeParticipants() {
    return status == ActivityStatus.CANCELLED || status == ActivityStatus.FINISHED;
  }
}
