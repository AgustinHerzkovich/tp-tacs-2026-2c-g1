package com.solnotfound.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.user.User;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  @Test
  void mapsUserToDTO() {
    User user = new User();
    user.setId("1");
    user.setName("Jane Doe");

    UserDTO dto = UserMapper.toDTO(user);

    assertThat(dto.id()).isEqualTo("1");
    assertThat(dto.name()).isEqualTo("Jane Doe");
  }

  @Test
  void mapsDTOToUser() {
    UserDTO dto = new UserDTO("1", "Jane Doe");

    User user = UserMapper.toEntity(dto);

    assertThat(user.getId()).isEqualTo("1");
    assertThat(user.getName()).isEqualTo("Jane Doe");
  }

  @Test
  void mapsNullValues() {
    assertThat(UserMapper.toDTO(null)).isNull();
    assertThat(UserMapper.toEntity(null)).isNull();
  }
}
