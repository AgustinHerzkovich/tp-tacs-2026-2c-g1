package com.solnotfound.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    String id,
    String activityId,
    String type,
    String title,
    String message,
    LocalDateTime createdAt) {}
