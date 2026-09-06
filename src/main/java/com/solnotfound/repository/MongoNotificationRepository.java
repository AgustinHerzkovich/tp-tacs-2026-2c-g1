package com.solnotfound.repository;

import com.solnotfound.entity.notification.Notification;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

interface MongoNotificationRepository extends MongoRepository<Notification, String> {

  @Query(value = "{ 'read': ?0, 'receiverUser': ?1 }", sort = "{ 'createdAt': -1 }")
  List<Notification> findByReadAndReceiverUserId(Boolean read, String receiverUserId);
}
