package com.solnotfound.repository;

import com.solnotfound.entity.notifications.Notification;
import java.util.List;
import java.util.Optional;

public interface INotificationRepository {

  Notification save(Notification notification);

  List<Notification> saveAll(Iterable<Notification> notifications);

  Optional<Notification> findById(String id);

  List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUser);
}
