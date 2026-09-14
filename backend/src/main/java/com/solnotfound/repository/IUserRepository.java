package com.solnotfound.repository;

import com.solnotfound.entity.user.User;

public interface IUserRepository {

  User findOrCreate(String id);

  User save(User user);
}
