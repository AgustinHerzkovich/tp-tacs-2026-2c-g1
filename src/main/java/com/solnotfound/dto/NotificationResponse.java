package com.solnotfound.dto;

import com.solnotfound.entity.notifications.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
  String id,
  String activityId, //TODO: Chequear si combiene el Id o el ResponseActivity (para el front)
  NotificationType type,
  String title,
  String message,
  LocalDateTime createdAt
) {}
