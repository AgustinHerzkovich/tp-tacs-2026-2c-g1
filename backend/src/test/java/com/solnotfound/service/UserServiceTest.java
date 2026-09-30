package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.dto.TelegramLinkCodeResponse;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.user.User;
import com.solnotfound.exception.ErrorCode;
import com.solnotfound.exception.InvalidTelegramLinkCodeException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.InMemoryTelegramLinkCodeRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");
  private static final Duration TTL = Duration.ofMinutes(10);

  private InMemoryUserRepository userRepository;
  private InMemoryTelegramLinkCodeRepository linkCodeRepository;
  private UserService service;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserRepository();
    linkCodeRepository = new InMemoryTelegramLinkCodeRepository();
    service = serviceAt(NOW);
  }

  private UserService serviceAt(Instant instant) {
    return new UserService(
        userRepository, linkCodeRepository, TTL, Clock.fixed(instant, ZoneOffset.UTC));
  }

  @Test
  void returnsUserLinkedToTelegramChat() {
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    user.setTelegramChatId(123456789L);
    userRepository.save(user);
    userRepository.save(User.withId("user-2"));

    UserDTO dto = service.getUserByTelegramChatId(123456789L);

    assertThat(dto).isEqualTo(new UserDTO("user-1", "Jane Doe"));
  }

  @Test
  void throwsWhenNoUserIsLinkedToTelegramChat() {
    assertThatThrownBy(() -> service.getUserByTelegramChatId(987L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void rejectsMissingTelegramChatId() {
    assertThatThrownBy(() -> service.getUserByTelegramChatId(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.createTelegramLinkCode(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void issuesRandomCodesThatExpireAfterTheConfiguredTtl() {
    TelegramLinkCodeResponse first = service.createTelegramLinkCode(123L);
    TelegramLinkCodeResponse second = service.createTelegramLinkCode(123L);

    assertThat(first.code()).hasSizeGreaterThanOrEqualTo(43).isNotEqualTo(second.code());
    assertThat(first.expiresAt()).isEqualTo(NOW.plus(TTL));
  }

  @Test
  void linksTheChatOfTheCodeToUserAndKeepsExistingName() {
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    userRepository.save(user);
    String code = service.createTelegramLinkCode(123L).code();

    UserDTO linked = service.linkTelegramChat("user-1", "Token Name", code);

    assertThat(linked).isEqualTo(new UserDTO("user-1", "Jane Doe"));
    assertThat(service.getUserByTelegramChatId(123L)).isEqualTo(linked);
  }

  @Test
  void linkingCreatesUnknownUserWithTokenName() {
    String code = service.createTelegramLinkCode(123L).code();

    UserDTO linked = service.linkTelegramChat("new-user", "New User", code);

    assertThat(linked).isEqualTo(new UserDTO("new-user", "New User"));
  }

  @Test
  void aCodeCanBeRedeemedOnlyOnce() {
    String code = service.createTelegramLinkCode(123L).code();
    service.linkTelegramChat("user-1", "One", code);

    // A second user replaying the same link must not take the chat over.
    assertThatThrownBy(() -> service.linkTelegramChat("user-2", "Two", code))
        .isInstanceOf(InvalidTelegramLinkCodeException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.TELEGRAM_LINK_CODE_INVALID);
    assertThat(service.getUserByTelegramChatId(123L).id()).isEqualTo("user-1");
  }

  @Test
  void rejectsExpiredCodes() {
    String code = service.createTelegramLinkCode(123L).code();

    assertThatThrownBy(() -> serviceAt(NOW.plus(TTL)).linkTelegramChat("user-1", "One", code))
        .isInstanceOf(InvalidTelegramLinkCodeException.class);
    assertThatThrownBy(() -> service.getUserByTelegramChatId(123L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void rejectsUnknownCodesSuchAsARawChatId() {
    assertThatThrownBy(() -> service.linkTelegramChat("user-1", "One", "123"))
        .isInstanceOf(InvalidTelegramLinkCodeException.class);
  }

  @Test
  void storesOnlyTheHashOfTheCode() {
    String code = service.createTelegramLinkCode(123L).code();

    assertThat(linkCodeRepository.stored())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.codeHash()).isNotEqualTo(code).matches("[0-9a-f]{64}");
              assertThat(stored.chatId()).isEqualTo(123L);
            });
  }

  @Test
  void linkingMovesChatFromPreviousUser() {
    service.linkTelegramChat("user-1", "One", service.createTelegramLinkCode(123L).code());

    service.linkTelegramChat("user-2", "Two", service.createTelegramLinkCode(123L).code());

    assertThat(service.getUserByTelegramChatId(123L).id()).isEqualTo("user-2");
    assertThat(userRepository.findOrCreate("user-1").getTelegramChatId()).isNull();
  }
}
