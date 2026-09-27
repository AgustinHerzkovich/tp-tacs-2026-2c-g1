package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.user.User;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  @Test
  void returnsUserLinkedToTelegramChat() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    user.setTelegramChatId(123456789L);
    repository.save(user);
    repository.save(User.withId("user-2"));

    UserDTO dto = new UserService(repository).getUserByTelegramChatId(123456789L);

    assertThat(dto).isEqualTo(new UserDTO("user-1", "Jane Doe"));
  }

  @Test
  void throwsWhenNoUserIsLinkedToTelegramChat() {
    UserService service = new UserService(new InMemoryUserRepository());

    assertThatThrownBy(() -> service.getUserByTelegramChatId(987L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void rejectsMissingTelegramChatId() {
    UserService service = new UserService(new InMemoryUserRepository());

    assertThatThrownBy(() -> service.getUserByTelegramChatId(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void linksTelegramChatToUserAndKeepsExistingName() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    User user = User.withId("user-1");
    user.setName("Jane Doe");
    repository.save(user);
    UserService service = new UserService(repository);

    UserDTO linked = service.linkTelegramChat("user-1", "Token Name", 123L);

    assertThat(linked).isEqualTo(new UserDTO("user-1", "Jane Doe"));
    assertThat(service.getUserByTelegramChatId(123L)).isEqualTo(linked);
  }

  @Test
  void linkingCreatesUnknownUserWithTokenName() {
    UserService service = new UserService(new InMemoryUserRepository());

    UserDTO linked = service.linkTelegramChat("new-user", "New User", 123L);

    assertThat(linked).isEqualTo(new UserDTO("new-user", "New User"));
  }

  @Test
  void linkingMovesChatFromPreviousUser() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserService service = new UserService(repository);
    service.linkTelegramChat("user-1", "One", 123L);

    service.linkTelegramChat("user-2", "Two", 123L);

    assertThat(service.getUserByTelegramChatId(123L).id()).isEqualTo("user-2");
    assertThat(repository.findOrCreate("user-1").getTelegramChatId()).isNull();
  }

  @Test
  void linkingRejectsMissingChatId() {
    UserService service = new UserService(new InMemoryUserRepository());

    assertThatThrownBy(() -> service.linkTelegramChat("user-1", "One", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
