package com.solnotfound.repository;

import com.solnotfound.entity.notification.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotificationRepository {

  Notification save(Notification notification);

  List<Notification> saveAll(Iterable<Notification> notifications);

  Optional<Notification> findById(String id);

  List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUser);

  Page<Notification> findByReadAndReceiverUserId(
      Boolean read, String receiverUser, Pageable pageable);
}
