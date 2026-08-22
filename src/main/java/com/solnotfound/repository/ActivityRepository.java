package com.solnotfound.repository;

import com.solnotfound.entity.Activity;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository { // TODO: Por ahora se guardan en memoria
  private final Map<String, Activity> activities = new ConcurrentHashMap<>();

  public void save(Activity activity) {
    activities.put(activity.getId(), activity);
  }

  public List<Activity> findAll() {
    return List.copyOf(activities.values());
  }

  public Activity findById(String id) {
    return activities.get(id);
  }
}
