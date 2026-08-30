package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Notification {
  @Setter private String id;
  @Setter private LocalDateTime createdAt;

  private final String receiverUser;
  private final String activityId;

  private final NotificationType type;
  private final String title;
  private final String message;

  @Setter private NotificationStatus status;

  private boolean read;

  public Notification(String receiverUser, Activity activity, NotificationType type) {
    this.receiverUser = receiverUser;
    this.activityId = activity.getId();
    this.type = type;
    this.createdAt = LocalDateTime.now();

    this.message = type.generateMessage(activity);
    this.title = type.generateTitle(activity);

    this.status = NotificationStatus.PENDING;
    this.read = false;
  }

  public void setAsRead() {
    this.read = true;
  }
}
