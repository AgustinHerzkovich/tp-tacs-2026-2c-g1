package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
}
