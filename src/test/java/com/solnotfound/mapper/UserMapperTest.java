package com.solnotfound.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.User;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  @Test
  void mapsUserToDTO() {
    User user = new User();
    user.setId("1");
    user.setName("Jane Doe");
    user.setEmail("jane@example.com");

    UserDTO dto = UserMapper.toDTO(user);

    assertThat(dto.id()).isEqualTo("1");
    assertThat(dto.name()).isEqualTo("Jane Doe");
    assertThat(dto.email()).isEqualTo("jane@example.com");
  }

  @Test
  void mapsDTOToUser() {
    UserDTO dto = new UserDTO("1", "Jane Doe", "jane@example.com");

    User user = UserMapper.toEntity(dto);

    assertThat(user.getId()).isEqualTo("1");
    assertThat(user.getName()).isEqualTo("Jane Doe");
    assertThat(user.getEmail()).isEqualTo("jane@example.com");
  }

  @Test
  void mapsNullValues() {
    assertThat(UserMapper.toDTO(null)).isNull();
    assertThat(UserMapper.toEntity(null)).isNull();
  }
}
