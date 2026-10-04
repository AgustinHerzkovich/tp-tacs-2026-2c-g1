package com.solnotfound.repository;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.entity.activity.Activity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IActivityRepository {

  void save(Activity activity);

  List<Activity> findAll();

  List<Activity> findActive();

  Activity findById(String id);

  void deleteAll();

  List<Activity> findActivitiesByOrganizerId(String organizerId);

  List<Activity> findActivitiesByParticipantId(String participantId);

  Page<Activity> search(ActivityFilterDTO filter, Pageable pageable);

  Page<Activity> findActivitiesByOrganizerId(
      String organizerId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable);

  Page<Activity> findActivitiesByParticipantId(
      String participantId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable);

  /**
   * Atomically adds a participant if, at the moment of the update, the activity still accepts
   * participants, has a free spot and the user is not already in it. Concurrent requests for the
   * last spot therefore cannot both succeed, and nothing else in the activity is overwritten.
   *
   * @param activityId activity to join
   * @param userId identifier of the participant
   * @return {@code true} when the participant was added; {@code false} when any condition failed
   */
  boolean addParticipant(String activityId, String userId);

  /**
   * Atomically removes a participant, leaving the rest of the activity untouched. Removing a user
   * who is not a participant does nothing.
   *
   * @param activityId activity to leave
   * @param userId identifier of the participant
   */
  void removeParticipant(String activityId, String userId);

  /**
   * Stores a time zone on every activity that has none, i.e. those created before the zone was
   * persisted. Activities that already have a zone are left untouched.
   *
   * @param timeZone IANA zone identifier to assign
   * @return number of activities updated
   */
  long assignTimeZoneWhereMissing(String timeZone);
}
