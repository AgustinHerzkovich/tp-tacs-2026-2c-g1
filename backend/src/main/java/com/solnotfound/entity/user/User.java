package com.solnotfound.entity.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class User {
  @Id private String id;
  private String name;

  public static User withId(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
