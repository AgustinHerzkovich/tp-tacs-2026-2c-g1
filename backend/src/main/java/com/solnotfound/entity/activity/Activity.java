package com.solnotfound.entity.activity;

import com.solnotfound.entity.user.User;
import com.solnotfound.entity.weather.WeatherCondition;
import com.solnotfound.exception.ErrorCode;
import com.solnotfound.exception.IllegalStateActivityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "activities")
// Paged listings filter by organizer or participant and sort by dateTime; these indexes serve both
// the filter and the sort (their prefix also covers the plain organizer/participant lookups).
@CompoundIndex(name = "organizer_date_time", def = "{'organizer': 1, 'dateTime': 1}")
@CompoundIndex(name = "participants_date_time", def = "{'participants': 1, 'dateTime': 1}")
public class Activity {

  @Id @Getter @Setter private String id;
  @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private ActivityType type;
  @Getter @Setter private Location location;
  @Indexed @Getter @Setter private LocalDateTime dateTime;
  @Getter @Setter private Integer minParticipants;
  @Getter @Setter private Integer maxParticipants;

  @DocumentReference(lazy = true)
  private List<User> participants = new ArrayList<>();

  private List<WeatherCondition> weatherConditions = List.of();
  private List<String> imageKeys = List.of();

  @Setter @Getter
  private Integer
      anticipationWindow; // hecho en horas, cantidad de tiempo antes de la actividad para chequear

  @Setter @Getter private ReprogramationRange reprogramationRange;
  @Indexed @Getter private ActivityStatus status = ActivityStatus.CONFIRMED;
  private List<ActivityStatus> statusHistory = new ArrayList<>(List.of(ActivityStatus.CONFIRMED));

  /**
   * IANA zone in which {@link #dateTime} is a wall-clock reading (the organizer's zone when the
   * activity was created). Null for activities created before this field existed.
   */
  @Getter @Setter private String timeZone;

  @Getter private LocalDateTime startingSoonNotificationDateTime;

  @DocumentReference(lazy = true)
  private User organizer;

  // las condiciones del clima y avisar a los usuarios

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "The activity aggregate intentionally exposes its organizer entity")
  public synchronized User getOrganizer() {
    return organizer;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "The activity aggregate intentionally references its organizer entity")
  public synchronized void setOrganizer(User organizer) {
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

  public List<String> getImageKeys() {
    return List.copyOf(imageKeys);
  }

  public void setImageKeys(List<String> imageKeys) {
    this.imageKeys = List.copyOf(imageKeys);
  }

  public synchronized List<User> getParticipants() {
    return List.copyOf(participants);
  }

  /**
   * Indicates whether another participant can join according to capacity and activity status.
   *
   * @return {@code true} when the activity accepts at least one more participant
   */
  public synchronized Boolean getAvailability() {
    return !cannotChangeParticipants() && participants.size() < maxParticipants;
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
    status = newStatus;
    statusHistory.add(newStatus);
  }

  /**
   * Adds a participant if they are not already registered and capacity remains.
   *
   * @param user participant to add
   * @throws IllegalStateActivityException if the activity no longer accepts changes or is full
   */
  public synchronized void addParticipant(User user) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be added to an activity in that status.");
    }

    boolean alreadyParticipating =
        participants.stream().anyMatch(participant -> participant.getId().equals(user.getId()));

    if (alreadyParticipating) {
      return;
    }

    boolean isFull = participants.size() >= maxParticipants;

    if (isFull) {
      throw new IllegalStateActivityException(
          ErrorCode.ACTIVITY_FULL, "Activity has no available spots.");
    }

    participants.add(user);
  }

  /**
   * Adds an identifier-only participant. Persistence flows should use {@link #addParticipant(User)}
   * with an already persisted user reference.
   *
   * @param userId identifier of the participant
   */
  public synchronized void addParticipant(String userId) {
    addParticipant(User.withId(userId));
  }

  /**
   * Removes a participant when present.
   *
   * @param userId identifier of the participant
   * @throws IllegalStateActivityException if the activity no longer accepts participant changes
   */
  public synchronized void removeParticipant(String userId) {
    if (cannotChangeParticipants()) {
      throw new IllegalStateActivityException(
          "Participants cannot be removed to an activity in that status.");
    }

    participants.removeIf(participant -> participant.getId().equals(userId));
  }

  /**
   * Current wall-clock time in the activity's own zone, the only "now" that can be compared with
   * {@link #dateTime}. The server clock runs in UTC in containers, so comparing against it shifted
   * every deadline by the organizer's offset. Activities without a stored zone fall back to the JVM
   * default zone.
   *
   * @return the current local date-time where the activity takes place
   */
  public LocalDateTime now() {
    return LocalDateTime.now(timeZone == null ? ZoneId.systemDefault() : ZoneId.of(timeZone));
  }

  /**
   * Indicates whether the forecast must be evaluated now: the activity is inside its anticipation
   * window and is still going ahead on its current date. It stays {@code true} on every run inside
   * the window, so a forecast that worsens after a first good check is still detected, and a
   * rescheduled activity is watched again on its new date. A proposed activity is excluded because
   * its votation is already deciding the new date.
   *
   * @return {@code true} when the weather check should run
   */
  public boolean isTimeToCheckWeatherConditions() {
    if (status != ActivityStatus.CONFIRMED && status != ActivityStatus.RESCHEDULED) {
      return false;
    }
    LocalDateTime now = now();
    LocalDateTime windowStart = dateTime.minusHours(anticipationWindow);
    return !windowStart.isAfter(now) && !dateTime.isBefore(now);
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

  public synchronized boolean isAParticipant(User user) {
    return participants.contains(user);
  }

  public synchronized Optional<User> findParticipant(String userId) {
    return participants.stream().filter(user -> user.getId().equals(userId)).findFirst();
  }

  /**
   * Finds an organizer or participant by identifier.
   *
   * @param userId identifier to find
   * @return the user belonging to this activity, or empty when the identifier is unrelated
   */
  public synchronized Optional<User> findOrganizerOrParticipant(String userId) {
    if (organizer != null && organizer.getId().equals(userId)) {
      return Optional.of(organizer);
    }
    return participants.stream().filter(user -> user.getId().equals(userId)).findFirst();
  }

  /**
   * Checks if the activity is near its start time for notification purposes.
   *
   * @param now the current time
   * @param notificationThreshold the number of hours before the activity starts to trigger a
   *     notification
   * @return {@code true} if the activity is near its start time
   */
  public boolean nearStart(LocalDateTime now, int notificationThreshold) {
    return nearStart(now, java.time.Duration.ofHours(notificationThreshold));
  }

  /**
   * Checks whether the activity is inside the configured notification window, including the exact
   * window start and excluding the activity start itself.
   *
   * @param now current time
   * @param notificationThreshold duration before the activity starts
   * @return {@code true} when a starting-soon notification is due
   */
  public boolean nearStart(LocalDateTime now, java.time.Duration notificationThreshold) {
    LocalDateTime notificationTime = dateTime.minus(notificationThreshold);
    return !now.isBefore(notificationTime) && now.isBefore(dateTime);
  }

  /**
   * Marks the starting-soon notification as sent for the current scheduled date. A later
   * rescheduling changes the date and therefore makes the activity eligible for a new reminder.
   */
  public void markStartingSoonNotificationSent() {
    startingSoonNotificationDateTime = dateTime;
  }

  /**
   * Indicates whether a starting-soon notification was already sent for the current scheduled date.
   *
   * @return {@code true} when the current schedule has already been notified
   */
  public boolean wasStartingSoonNotificationSent() {
    return dateTime.equals(startingSoonNotificationDateTime);
  }
}
