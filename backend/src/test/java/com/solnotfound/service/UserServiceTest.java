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
}
