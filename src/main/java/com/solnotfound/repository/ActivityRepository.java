package com.solnotfound.repository;

import com.solnotfound.entity.Activity;
import java.util.List;

public interface ActivityRepository {
  public void save(Activity activity);

  public List<Activity> findAll();

  public Activity findById(String id);

  public List<Activity> findActivitiesByOrganizerId(String organizerId);

  public List<Activity> findActivitiesByParticipantId(String participantId);
}
