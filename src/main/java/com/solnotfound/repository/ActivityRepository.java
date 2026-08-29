package com.solnotfound.repository;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository implements IActivityRepository {
  private final Map<String, Activity> activities = new ConcurrentHashMap<>();

  @Override
  public void save(Activity activity) {
    activities.put(activity.getId(), activity);
  }

  @Override
  public List<Activity> findAll() {
    return List.copyOf(activities.values());
  }

  @Override
  public List<Activity> findActive() {
    return activities.values().stream()
        .filter(
            activity ->
                activity.getStatus() != ActivityStatus.CANCELLED
                    && activity.getStatus() != ActivityStatus.FINISHED)
        .toList();
  }

  @Override
  public Activity findById(String id) {
    return activities.get(id);
  }

  @Override
  public void deleteAll() {
    activities.clear();
  }

  public List<Activity> findActivitiesByOrganizerId(String organizerId) {
    return activities.values().stream()
      .filter(activity -> activity.getOrganizer().getId().equals(organizerId))
      .toList();
  }

  public List<Activity> findActivitiesByParticipantId(String participantId) {
    return activities.values().stream()
      .filter(activity ->
        activity.getParticipants().stream()
          .anyMatch(participant -> participant.getId().equals(participantId)))
      .toList();
  }

}
