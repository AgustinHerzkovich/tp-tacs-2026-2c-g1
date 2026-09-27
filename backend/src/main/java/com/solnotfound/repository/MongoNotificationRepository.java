package com.solnotfound.repository;

import com.solnotfound.entity.notification.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

interface MongoNotificationRepository extends MongoRepository<Notification, String> {

  @Query(value = "{ 'read': ?0, 'receiverUser': ?1 }", sort = "{ 'createdAt': -1 }")
  List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUserId);

  @Query(value = "{ 'read': ?0, 'receiverUser': ?1 }", sort = "{ 'createdAt': -1 }")
  Page<Notification> findByReadAndReceiverUserId(
      Boolean read, String receiverUserId, Pageable pageable);

  @Query(value = "{ 'receiverUser': ?0 }", sort = "{ 'read': 1, 'createdAt': -1 }")
  List<Notification> findByReceiverUserId(String receiverUserId);

  @Query(value = "{ 'receiverUser': ?0 }", sort = "{ 'read': 1, 'createdAt': -1 }")
  Page<Notification> findByReceiverUserId(String receiverUserId, Pageable pageable);
}
