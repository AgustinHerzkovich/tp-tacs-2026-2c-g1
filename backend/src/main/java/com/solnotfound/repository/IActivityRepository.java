package com.solnotfound.repository;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.entity.activity.Activity;
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

  Page<Activity> findActivitiesByOrganizerId(String organizerId, Pageable pageable);

  Page<Activity> findActivitiesByParticipantId(String participantId, Pageable pageable);
}
