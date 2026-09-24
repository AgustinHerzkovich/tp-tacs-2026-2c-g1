package com.solnotfound.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.entity.user.User;
import com.solnotfound.repository.IUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "telegram.api-token=test-telegram-token")
@AutoConfigureMockMvc
class TelegramSecurityTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IUserRepository userRepository;

  @Test
  void telegramEndpointsRequireApiToken() throws Exception {
    mockMvc.perform(get("/users/telegram/123")).andExpect(status().isUnauthorized());
  }

  @Test
  void telegramEndpointsRejectInvalidApiToken() throws Exception {
    mockMvc
        .perform(get("/users/telegram/123").header("X-Api-Token", "wrong-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void telegramEndpointsRejectKeycloakUsers() throws Exception {
    mockMvc.perform(get("/users/telegram/123").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void telegramEndpointsAllowValidApiToken() throws Exception {
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    when(userRepository.findByTelegramChatId(123L)).thenReturn(Optional.of(user));

    mockMvc
        .perform(get("/users/telegram/123").header("X-Api-Token", "test-telegram-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("user-1"));
  }

  @Test
  void apiTokenDoesNotGrantAccessToOtherEndpoints() throws Exception {
    mockMvc
        .perform(get("/notifications").header("X-Api-Token", "test-telegram-token"))
        .andExpect(status().isUnauthorized());
  }
}
