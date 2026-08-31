package com.solnotfound.entity.user;

import lombok.Data;

@Data
public class User {
  private String id;
  private String name;
  private String email;

  public static User withId(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
