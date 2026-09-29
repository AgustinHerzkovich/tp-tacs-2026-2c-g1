package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

interface MongoUserRepository extends MongoRepository<User, String> {

  Optional<User> findByTelegramChatId(Long telegramChatId);
}
