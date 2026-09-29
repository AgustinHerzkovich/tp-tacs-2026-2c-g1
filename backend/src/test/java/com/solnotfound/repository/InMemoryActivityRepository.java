package com.solnotfound.repository;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class InMemoryActivityRepository implements IActivityRepository {
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

  @Override
  public List<Activity> findActivitiesByOrganizerId(String organizerId) {
    return activities.values().stream()
        .filter(
            activity ->
                activity.getOrganizer() != null
                    && organizerId.equals(activity.getOrganizer().getId()))
        .toList();
  }

  @Override
  public List<Activity> findActivitiesByParticipantId(String participantId) {
    return activities.values().stream()
        .filter(
            activity ->
                activity.getParticipants().stream()
                    .anyMatch(participant -> participantId.equals(participant.getId())))
        .toList();
  }

  @Override
  public Page<Activity> search(ActivityFilterDTO filter, Pageable pageable) {
    return page(findAll(), pageable);
  }

  @Override
  public Page<Activity> findActivitiesByOrganizerId(
      String organizerId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
    return page(
        filterByDateRange(findActivitiesByOrganizerId(organizerId), dateFrom, dateTo), pageable);
  }

  @Override
  public Page<Activity> findActivitiesByParticipantId(
      String participantId, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
    return page(
        filterByDateRange(findActivitiesByParticipantId(participantId), dateFrom, dateTo),
        pageable);
  }

  private List<Activity> filterByDateRange(
      List<Activity> source, LocalDateTime dateFrom, LocalDateTime dateTo) {
    return source.stream()
        .filter(activity -> dateFrom == null || !activity.getDateTime().isBefore(dateFrom))
        .filter(activity -> dateTo == null || !activity.getDateTime().isAfter(dateTo))
        .toList();
  }

  private Page<Activity> page(List<Activity> source, Pageable pageable) {
    int start = Math.min((int) pageable.getOffset(), source.size());
    int end = Math.min(start + pageable.getPageSize(), source.size());
    return new PageImpl<>(source.subList(start, end), pageable, source.size());
  }
}
