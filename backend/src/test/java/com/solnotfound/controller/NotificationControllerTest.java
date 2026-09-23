package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.dto.PageResponse;
import com.solnotfound.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class NotificationControllerTest {

  @Test
  void usesJwtSubjectToGetNotifications() {
    NotificationService service = mock(NotificationService.class);
    when(service.getNotificationsByUser(
            org.mockito.ArgumentMatchers.eq("user-auth-123"),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageResponse<>(java.util.List.of(), 0, 10, 0, 0, true, true));
    NotificationController controller = new NotificationController(service);

    var response = controller.getNotifications(authentication("user-auth-123"), 0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(service)
        .getNotificationsByUser(
            org.mockito.ArgumentMatchers.eq("user-auth-123"),
            org.mockito.ArgumentMatchers.any(Pageable.class));
  }

  @Test
  void usesJwtSubjectToMarkNotificationAsRead() {
    NotificationService service = mock(NotificationService.class);
    NotificationController controller = new NotificationController(service);

    var response = controller.markAsRead("notif-1", authentication("user-auth-123"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(service).markAsRead("notif-1", "user-auth-123");
  }

  private JwtAuthenticationToken authentication(String subject) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
    return new JwtAuthenticationToken(jwt);
  }
}
