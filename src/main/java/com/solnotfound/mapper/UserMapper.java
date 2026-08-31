package com.solnotfound.mapper;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.user.User;

public final class UserMapper {

  private UserMapper() {}

  public static UserDTO toDTO(User user) {
    if (user == null) {
      return null;
    }

    return new UserDTO(user.getId(), user.getName(), user.getEmail());
  }

  public static User toEntity(UserDTO userDTO) {
    if (userDTO == null) {
      return null;
    }

    User user = new User();
    user.setId(userDTO.id());
    user.setName(userDTO.name());
    user.setEmail(userDTO.email());

    return user;
  }
}
