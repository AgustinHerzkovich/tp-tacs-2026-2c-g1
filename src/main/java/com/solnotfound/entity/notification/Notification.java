package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.user.User;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Notification {
  @Setter private String id;
  @Setter private LocalDateTime createdAt;

  @Getter(
      onMethod_ =
          @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
              value = "EI_EXPOSE_REP",
              justification = "Notification intentionally exposes its receiver aggregate"))
  private final User receiverUser;

  @Getter(
      onMethod_ =
          @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
              value = "EI_EXPOSE_REP",
              justification = "Notification intentionally exposes its activity aggregate"))
  private final Activity activity;

  private final NotificationType type;
  private final String title;
  private final String message;

  private boolean read;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Notification intentionally stores receiver and activity aggregate references")
  public Notification(User receiverUser, Activity activity, NotificationType type) {
    this.receiverUser = receiverUser;
    this.activity = activity;
    this.type = type;
    this.createdAt = LocalDateTime.now();

    this.message = type.generateMessage(activity);
    this.title = type.generateTitle(activity);

    this.read = false;
  }

  public void setAsRead() {
    this.read = true;
  }
}
