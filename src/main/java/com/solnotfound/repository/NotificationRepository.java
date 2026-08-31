package com.solnotfound.repository;

import com.solnotfound.entity.notification.Notification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository implements INotificationRepository {

  private final Map<String, Notification> storage = new ConcurrentHashMap<>();

  /**
   * Assigns missing identifiers and creation timestamps before storing a notification.
   *
   * @param notification notification to persist
   * @return the stored notification
   */
  @Override
  public Notification save(Notification notification) {
    if (notification.getId() == null || notification.getId().isBlank()) {
      notification.setId(UUID.randomUUID().toString());
    }
    if (notification.getCreatedAt() == null) {
      notification.setCreatedAt(LocalDateTime.now());
    }
    storage.put(notification.getId(), notification);
    return notification;
  }

  /**
   * Stores every supplied notification and returns an immutable list of stored values.
   *
   * @param notifications notifications to persist
   * @return stored notifications in iteration order
   */
  @Override
  public List<Notification> saveAll(Iterable<Notification> notifications) {
    List<Notification> savedNotifications = new ArrayList<>();
    for (Notification notification : notifications) {
      savedNotifications.add(save(notification));
    }
    return List.copyOf(savedNotifications);
  }

  @Override
  public Optional<Notification> findById(String id) {
    return Optional.ofNullable(storage.get(id));
  }

  /**
   * Finds notifications for a receiver with the requested read state, newest first.
   *
   * @param read required read state
   * @param receiverUserId receiver identifier
   * @return matching notifications ordered by descending creation time
   */
  @Override
  public List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUserId) {
    return storage.values().stream()
        .filter(n -> Objects.equals(n.getReceiverUser().getId(), receiverUserId))
        .filter(n -> Objects.equals(n.isRead(), read))
        .sorted(
            Comparator.comparing(
                Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .collect(Collectors.toList());
  }
}
