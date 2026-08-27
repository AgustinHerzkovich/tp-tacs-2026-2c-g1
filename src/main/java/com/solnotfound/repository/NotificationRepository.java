package com.solnotfound.repository;

import com.solnotfound.entity.notifications.Notification;
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

  @Override
  public List<Notification> saveAll(Iterable<Notification> notifications) {
    for (Notification notification : notifications) {
      storage.put(notification.getId(), notification);
    }
    return new ArrayList<>(storage.values());
  }

  @Override
  public Optional<Notification> findById(String id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public List<Notification> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUserId) {
    return storage.values().stream()
      .filter(n -> Objects.equals(n.getReceiverUser(), receiverUserId))
      .filter(n -> Objects.equals(n.isRead(), read))
      .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
      .collect(Collectors.toList());
  }
}
