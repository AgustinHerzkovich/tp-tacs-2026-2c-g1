package com.solnotfound.listener;

import com.solnotfound.entity.Activity;
import com.solnotfound.entity.notifications.NotificationType;

public record ActivityNotificationEvent(String activityId, NotificationType type) {

  public static ActivityNotificationEvent from(Activity activity, NotificationType type) {
    return new ActivityNotificationEvent(activity.getId(), type);
  }
}
