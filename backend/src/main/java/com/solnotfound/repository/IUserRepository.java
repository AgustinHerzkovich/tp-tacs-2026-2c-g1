package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import com.solnotfound.entity.user.WeatherProvider;
import java.util.Optional;

public interface IUserRepository {

  User findOrCreate(String id);

  User save(User user);

  /**
   * Sets the display name of a user in a single atomic write, creating the user when it does not
   * exist yet. Only the name is written, so a concurrent change to the rest of the user is kept.
   *
   * @param id JWT subject used as the persistent identifier
   * @param name display name to store
   */
  void rememberName(String id, String name);

  /**
   * Sets the weather provider chosen by a user in a single atomic write, creating the user when it
   * does not exist yet. Only that field is written.
   *
   * @param id JWT subject used as the persistent identifier
   * @param provider weather provider to store
   */
  void saveWeatherProvider(String id, WeatherProvider provider);

  Optional<User> findByTelegramChatId(Long telegramChatId);

  User linkTelegramChat(String userId, String name, Long telegramChatId);
}
