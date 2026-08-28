package com.solnotfound.listener;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.notifications.NotificationType;

public record ActivityNotificationEvent(
  Activity activity,
  NotificationType type
) { }
