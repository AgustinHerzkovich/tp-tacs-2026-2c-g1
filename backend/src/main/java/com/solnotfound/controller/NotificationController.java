package com.solnotfound.controller;

import com.solnotfound.dto.NotificationResponse;
import com.solnotfound.dto.PageResponse;
import com.solnotfound.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException(
          "Page must be non-negative and size must be between 1 and 100");
    }
    return ResponseEntity.ok(
        notificationService.getNotificationsByUser(
            jwt(authentication).getSubject(), PageRequest.of(page, size)));
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
