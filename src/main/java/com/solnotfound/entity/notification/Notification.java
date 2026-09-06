package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.user.User;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Document(collection = "notifications")
@CompoundIndex(
    name = "receiver_read_created_at",
    def = "{'receiverUser': 1, 'read': 1, 'createdAt': -1}")
public class Notification {
  @Id @Setter private String id;
  @Setter private LocalDateTime createdAt;

  @Getter(
      onMethod_ =
          @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
              value = "EI_EXPOSE_REP",
              justification = "Notification intentionally exposes its receiver aggregate"))
  @DocumentReference(lazy = true)
  private final User receiverUser;

  @Getter(
      onMethod_ =
          @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
              value = "EI_EXPOSE_REP",
              justification = "Notification intentionally exposes its activity aggregate"))
  @DocumentReference(lazy = true)
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

  /** Reconstructs a persisted notification without regenerating its rendered content. */
  @PersistenceCreator
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Persistence reconstruction restores intentional document references")
  public Notification(
      String id,
      LocalDateTime createdAt,
      User receiverUser,
      Activity activity,
      NotificationType type,
      String title,
      String message,
      boolean read) {
    this.id = id;
    this.createdAt = createdAt;
    this.receiverUser = receiverUser;
    this.activity = activity;
    this.type = type;
    this.title = title;
    this.message = message;
    this.read = read;
  }

  public void setAsRead() {
    this.read = true;
  }
}
