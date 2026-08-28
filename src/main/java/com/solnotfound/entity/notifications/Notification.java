package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Notification {
  @Setter
  private String id;
  @Setter
  private LocalDateTime createdAt;

  private final String receiverUser;     // TODO: asociar a quien se le manda la notificacion (user)
  private final Activity activity;

  private final NotificationType type;
  private final String title;
  private final String message;

  @Setter
  private NotificationStatus status;

  private boolean read;

  public Notification(String id, String receiverUser, Activity activity, NotificationType type, LocalDateTime createdAt) {
    this.id = id;
    this.receiverUser = receiverUser;
    this.activity = activity;
    this.type = type;
    this.createdAt = createdAt;

    this.message = type.generateMessage(activity);
    this.title = type.generateTitle(activity);

    this.status = NotificationStatus.PENDING;
    this.read = false;
  }

  public void setAsRead() {
    this.read = true;
  }
}
