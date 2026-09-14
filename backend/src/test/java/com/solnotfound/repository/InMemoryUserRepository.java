package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements IUserRepository {
  private final Map<String, User> users = new ConcurrentHashMap<>();

  @Override
  public User findOrCreate(String id) {
    return users.computeIfAbsent(id, User::withId);
  }

  @Override
  public User save(User user) {
    users.put(user.getId(), user);
    return user;
  }
}
