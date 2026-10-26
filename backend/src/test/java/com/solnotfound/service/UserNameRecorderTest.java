package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.solnotfound.entity.user.User;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UserNameRecorderTest {

  @Test
  void storesTheNameOfAUserThatDidNotHaveOne() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    User legacy = repository.findOrCreate("user-1");

    new UserNameRecorder(repository).record(jwt("user-1", "Vale Ríos", "vale"));

    assertThat(legacy.getName()).isEqualTo("Vale Ríos");
  }

  @Test
  void createsTheUserWhenItDoesNotExistYet() {
    InMemoryUserRepository repository = new InMemoryUserRepository();

    new UserNameRecorder(repository).record(jwt("new-user", "Juan Pérez", "juan"));

    assertThat(repository.findOrCreate("new-user").getName()).isEqualTo("Juan Pérez");
  }

  @Test
  void fallsBackToTheUsernameWhenTheTokenHasNoFullName() {
    InMemoryUserRepository repository = new InMemoryUserRepository();

    new UserNameRecorder(repository).record(jwt("user-1", null, "vale"));

    assertThat(repository.findOrCreate("user-1").getName()).isEqualTo("vale");
  }

  @Test
  void writesOnlyOnceWhileTheNameDoesNotChange() {
    IUserRepository repository = mock(IUserRepository.class);
    UserNameRecorder recorder = new UserNameRecorder(repository);

    recorder.record(jwt("user-1", "Vale Ríos", "vale"));
    recorder.record(jwt("user-1", "Vale Ríos", "vale"));
    recorder.record(jwt("user-1", "Valeria Ríos", "vale"));

    verify(repository, times(1)).rememberName("user-1", "Vale Ríos");
    verify(repository, times(1)).rememberName("user-1", "Valeria Ríos");
  }

  @Test
  void ignoresMachineClientsAndTokensWithoutAName() {
    IUserRepository repository = mock(IUserRepository.class);
    UserNameRecorder recorder = new UserNameRecorder(repository);

    recorder.record(jwt("bot", null, "service-account-solnotfoundtelegrambot"));
    recorder.record(jwt("anonymous", null, null));

    verify(repository, never()).rememberName(any(), any());
  }

  private Jwt jwt(String subject, String name, String username) {
    Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject(subject);
    if (name != null) {
      builder.claim("name", name);
    }
    if (username != null) {
      builder.claim("preferred_username", username);
    }
    return builder.build();
  }
}
