package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.dto.LinkTelegramChatRequest;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserControllerTest {

  @Test
  void returnsUserLinkedToTelegramChat() {
    UserService service = mock(UserService.class);
    UserDTO user = new UserDTO("user-1", "Jane Doe");
    when(service.getUserByTelegramChatId(123456789L)).thenReturn(user);

    var response = new UserController(service).getUserByTelegramChatId(123456789L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(user);
  }

  @Test
  void linksTelegramChatToJwtSubjectUsingNameClaim() {
    UserService service = mock(UserService.class);
    UserDTO user = new UserDTO("user-auth-123", "Jane Doe");
    when(service.linkTelegramChat("user-auth-123", "Jane Doe", 123456789L)).thenReturn(user);
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-auth-123")
            .claim("name", "Jane Doe")
            .build();

    var response =
        new UserController(service)
            .linkTelegramChat(
                new LinkTelegramChatRequest(123456789L), new JwtAuthenticationToken(jwt));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(user);
  }

  @Test
  void fallsBackToPreferredUsernameWhenNameClaimIsMissing() {
    UserService service = mock(UserService.class);
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-auth-123")
            .claim("preferred_username", "jane")
            .build();

    new UserController(service)
        .linkTelegramChat(new LinkTelegramChatRequest(1L), new JwtAuthenticationToken(jwt));

    verify(service).linkTelegramChat("user-auth-123", "jane", 1L);
  }
}
