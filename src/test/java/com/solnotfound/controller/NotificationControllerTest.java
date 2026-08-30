package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class NotificationControllerTest {

  @Test
  void usesJwtSubjectToGetNotifications() {
    NotificationService service = mock(NotificationService.class);
    when(service.getNotificationsByUser("user-auth-123")).thenReturn(List.of());
    NotificationController controller = new NotificationController(service);

    var response = controller.getNotifications(authentication("user-auth-123"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(service).getNotificationsByUser("user-auth-123");
  }

  @Test
  void usesJwtSubjectToMarkNotificationAsRead() {
    NotificationService service = mock(NotificationService.class);
    NotificationController controller = new NotificationController(service);

    var response = controller.markAsRead("notif-1", authentication("user-auth-123"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(service).markAsRead("notif-1", "user-auth-123");
  }

  private JwtAuthenticationToken authentication(String subject) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
    return new JwtAuthenticationToken(jwt);
  }
}
