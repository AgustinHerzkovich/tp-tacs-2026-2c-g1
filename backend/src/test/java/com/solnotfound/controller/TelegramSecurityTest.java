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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "telegram.api-token=test-telegram-token")
@AutoConfigureMockMvc
class TelegramSecurityTest {

  private static final String API_TOKEN_HEADER = "X-Api-Token";
  private static final String API_TOKEN = "test-telegram-token";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IUserRepository userRepository;

  private void givenTelegramUserExists() {
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    when(userRepository.findByTelegramChatId(123L)).thenReturn(Optional.of(user));
  }

  @Test
  void telegramEndpointsRequireBothCredentials() throws Exception {
    mockMvc.perform(get("/users/telegram/123")).andExpect(status().isUnauthorized());
  }

  @Test
  void apiTokenAloneIsNotEnough() throws Exception {
    mockMvc
        .perform(get("/users/telegram/123").header(API_TOKEN_HEADER, API_TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void botJwtAloneIsNotEnough() throws Exception {
    mockMvc
        .perform(
            get("/users/telegram/123")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TELEGRAM_BOT"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void telegramEndpointsRejectInvalidApiToken() throws Exception {
    mockMvc
        .perform(
            get("/users/telegram/123")
                .header(API_TOKEN_HEADER, "wrong-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TELEGRAM_BOT"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void telegramEndpointsRejectRegularUsers() throws Exception {
    mockMvc
        .perform(
            get("/users/telegram/123")
                .header(API_TOKEN_HEADER, API_TOKEN)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void telegramEndpointsAllowBotRoleAndApiToken() throws Exception {
    givenTelegramUserExists();

    mockMvc
        .perform(
            get("/users/telegram/123")
                .header(API_TOKEN_HEADER, API_TOKEN)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TELEGRAM_BOT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("user-1"));
  }

  @Test
  void apiTokenDoesNotAuthenticateOtherEndpoints() throws Exception {
    mockMvc
        .perform(get("/notifications").header(API_TOKEN_HEADER, API_TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void botRoleDoesNotGrantAdminEndpoints() throws Exception {
    mockMvc
        .perform(
            get("/statistics/weather")
                .header(API_TOKEN_HEADER, API_TOKEN)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TELEGRAM_BOT"))))
        .andExpect(status().isForbidden());
  }
}
