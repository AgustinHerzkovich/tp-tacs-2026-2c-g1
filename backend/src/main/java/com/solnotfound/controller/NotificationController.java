package com.solnotfound.controller;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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

  @GetMapping
  public ResponseEntity<List<NotificationResponse>> getNotifications(
      Authentication authentication) {
    return ResponseEntity.ok(
        notificationService.getNotificationsByUser(jwt(authentication).getSubject()));
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable String id, Authentication authentication) {
    notificationService.markAsRead(id, jwt(authentication).getSubject());
    return ResponseEntity.ok().build();
  }

  private Jwt jwt(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) return jwt;
    throw new IllegalStateException("Authenticated principal must be a JWT");
  }
}
