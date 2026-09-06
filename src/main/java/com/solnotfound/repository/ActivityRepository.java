package com.solnotfound.repository;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository implements IActivityRepository {
  private final MongoActivityRepository repository;

  public ActivityRepository(MongoActivityRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Activity activity) {
    repository.save(activity);
  }

  @Override
  public List<Activity> findAll() {
    return repository.findAll();
  }

  @Override
  public List<Activity> findActive() {
    return repository.findByStatusNotIn(List.of(ActivityStatus.CANCELLED, ActivityStatus.FINISHED));
  }

  @Override
  public Activity findById(String id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public void deleteAll() {
    repository.deleteAll();
  }

  @Override
  public List<Activity> findActivitiesByOrganizerId(String organizerId) {
    return repository.findByOrganizerId(organizerId);
  }

  @Override
  public List<Activity> findActivitiesByParticipantId(String participantId) {
    return repository.findByParticipantId(participantId);
  }
}
