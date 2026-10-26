package com.solnotfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.entity.user.TelegramLinkCode;
import com.solnotfound.entity.user.User;
import com.solnotfound.repository.ITelegramLinkCodeRepository;
import com.solnotfound.repository.IUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Runs without MongoDB (also in the Docker image build), so the startup index creation, which needs
// a live server, is disabled like in StatisticsSecurityTest.
@SpringBootTest(
    properties = {
      "telegram.api-token=test-telegram-token",
      "spring.data.mongodb.auto-index-creation=false"
    })
@AutoConfigureMockMvc
class TelegramSecurityTest {

  private static final String API_TOKEN_HEADER = "X-Api-Token";
  private static final String API_TOKEN = "test-telegram-token";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IUserRepository userRepository;
  @MockitoBean private ITelegramLinkCodeRepository linkCodeRepository;

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

  @Test
  void regularUsersLinkTheirTelegramChatWithTheirJwtOnly() throws Exception {
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    when(linkCodeRepository.consume(any(), any()))
        .thenReturn(
            Optional.of(
                new TelegramLinkCode("hash", 123L, Instant.now().plus(Duration.ofMinutes(5)))));
    when(userRepository.linkTelegramChat(eq("user-1"), any(), eq(123L))).thenReturn(user);

    mockMvc
        .perform(
            put("/users/me/telegram")
                .with(jwt().jwt(token -> token.subject("user-1")))
                .contentType("application/json")
                .content("{\"code\":\"link-code\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("user-1"));
  }

  @Test
  void linkingTelegramChatRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            put("/users/me/telegram")
                .contentType("application/json")
                .content("{\"code\":\"link-code\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void onlyTheBotCanIssueLinkCodes() throws Exception {
    mockMvc.perform(post("/users/telegram/123/link-code")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/users/telegram/123/link-code")
                .header(API_TOKEN_HEADER, API_TOKEN)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void botIssuesLinkCodeForAChat() throws Exception {
    mockMvc
        .perform(
            post("/users/telegram/123/link-code")
                .header(API_TOKEN_HEADER, API_TOKEN)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TELEGRAM_BOT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.expiresAt").exists());
  }

  @Test
  void rejectsUnknownOrUsedLinkCodes() throws Exception {
    when(linkCodeRepository.consume(any(), any())).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/users/me/telegram")
                .with(jwt().jwt(token -> token.subject("user-1")))
                .contentType("application/json")
                .content("{\"code\":\"used-code\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TELEGRAM_LINK_CODE_INVALID"));
  }
}
