package com.solnotfound.controller;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/me")
  public ResponseEntity<List<NotificationResponse>> getNotifications() {

    // TODO: Implement the logic to get the authenticated user's ID
    String userId = "hardcodeado123";

    return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> markAsRead(@PathVariable String id) {

    // TODO: Implement the logic to get the authenticated user's ID
    String currentUserId = "hardcodeado123";

    notificationService.markAsRead(id, currentUserId);
    return ResponseEntity.ok().build();
  }

}
