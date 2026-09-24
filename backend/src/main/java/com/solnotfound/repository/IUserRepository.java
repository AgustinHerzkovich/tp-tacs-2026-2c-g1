package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import java.util.Optional;

public interface IUserRepository {

  User findOrCreate(String id);

  User save(User user);

  Optional<User> findByTelegramChatId(Long telegramChatId);
}
