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
  private List<User> participants = new ArrayList<>();
  private List<WeatherCondition> weatherConditions = List.of();

  @Setter @Getter
  private Integer
      anticipationWindow; // hecho en horas, cantidad de tiempo antes de la actividad para chequear

  @Setter @Getter private ReprogramationRange reprogramationRange;
  private Boolean availability = false;
  @Getter private ActivityStatus status = ActivityStatus.CONFIRMED;
  private List<ActivityStatus> statusHistory = new ArrayList<>(List.of(ActivityStatus.CONFIRMED));
  @Setter @Getter private Boolean weatherChecked = false;
  private User organizer;

  // las condiciones del clima y avisar a los usuarios

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "The activity aggregate intentionally exposes its organizer entity")
  public User getOrganizer() {
    return organizer;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "The activity aggregate intentionally references its organizer entity")
  public void setOrganizer(User organizer) {
    this.organizer = organizer;
  }

  public synchronized void setParticipants(List<User> participants) {
    this.participants = new ArrayList<>(participants);
  }

  public List<WeatherCondition> getWeatherConditions() {
    return List.copyOf(weatherConditions);
  }

  public void setWeatherConditions(List<WeatherCondition> weatherConditions) {
    this.weatherConditions = List.copyOf(weatherConditions);
  }

  public synchronized List<User> getParticipants() {
    return List.copyOf(participants);
  }

  public synchronized Boolean getAvailability() {
    return availability;
  }

  public List<ActivityStatus> getStatusHistory() {
    return List.copyOf(statusHistory);
  }

  /**
   * Changes the activity status and appends it to the status history.
   *
   * @param newStatus status to apply
   */
  public void setStatus(ActivityStatus newStatus) {
    // TODO: Validar transiciones permitidas entre estados.
    status = newStatus;
    statusHistory.add(newStatus);
  }

  /**
   * Adds a participant if they are not already registered and updates availability when the minimum
   * participant count is reached.
   *
   * @param userId identifier of the participant
   * @throws IllegalStateActivityException if the activity no longer accepts changes or is full
   */
  public synchronized void addParticipant(String userId) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be added to an activity in that status.");
    }

    boolean alreadyParticipating =
        participants.stream().anyMatch(participant -> participant.getId().equals(userId));

    if (alreadyParticipating) {
      return;
    }

    boolean isFull = participants.size() >= maxParticipants;

    if (isFull) {
      throw new IllegalStateActivityException("Activity has no available spots.");
    }

    participants.add(User.withId(userId));

    if (participants.size() >= minParticipants) {
      availability = true;
    }
  }

  /**
   * Removes a participant when present and updates availability if the activity drops below its
   * minimum participant count.
   *
   * @param userId identifier of the participant
   * @throws IllegalStateActivityException if the activity no longer accepts participant changes
   */
  public synchronized void removeParticipant(String userId) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be removed to an activity in that status.");
    }

    boolean removed = participants.removeIf(participant -> participant.getId().equals(userId));

    if (!removed) {
      return;
    }

    if (participants.size() < minParticipants) {
      availability = false;
    }
  }

  /**
   * Indicates whether the activity is inside its anticipation window and has not yet had its
   * forecast checked.
   *
   * @return {@code true} when the weather check should run
   */
  public boolean isTimeToCheckWeatherConditions() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime windowStart = dateTime.minusHours(anticipationWindow);

    boolean withinAnticipationWindow = !windowStart.isAfter(now) && !dateTime.isBefore(now);

    return (withinAnticipationWindow && !weatherChecked);
  }

  public void markWeatherChecked() {
    this.weatherChecked = true;
  }

  /**
   * Finishes the activity when its scheduled time has passed, unless it is already cancelled or
   * finished.
   *
   * @param now instant used to evaluate the activity
   * @return {@code true} when the status changed to {@link ActivityStatus#FINISHED}
   */
  public boolean finishIfPast(LocalDateTime now) {
    if (status != ActivityStatus.CANCELLED
        && status != ActivityStatus.FINISHED
        && dateTime.isBefore(now)) {
      setStatus(ActivityStatus.FINISHED);
      return true;
    }
    return false;
  }

  private boolean cannotChangeParticipants() {
    return status == ActivityStatus.CANCELLED || status == ActivityStatus.FINISHED;
  }
}
