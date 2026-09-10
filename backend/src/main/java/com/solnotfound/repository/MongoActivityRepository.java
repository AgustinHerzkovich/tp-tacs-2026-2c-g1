package com.solnotfound.repository;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

interface MongoActivityRepository extends MongoRepository<Activity, String> {

  List<Activity> findByStatusNotIn(Collection<ActivityStatus> statuses);

  @Query("{ 'organizer': ?0 }")
  List<Activity> findByOrganizerId(String organizerId);

  @Query("{ 'participants': ?0 }")
  List<Activity> findByParticipantId(String participantId);
}
