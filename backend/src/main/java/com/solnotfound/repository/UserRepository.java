package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository implements IUserRepository {
  private final MongoUserRepository repository;
  private final MongoTemplate mongoTemplate;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared MongoTemplate bean")
  public UserRepository(MongoUserRepository repository, MongoTemplate mongoTemplate) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Resolves a user by its authentication subject, creating the minimal profile when necessary.
   *
   * @param id JWT subject used as the persistent identifier
   * @return the existing or newly persisted user
   * @throws IllegalArgumentException when the identifier is blank
   */
  @Override
  public User findOrCreate(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("User identifier cannot be blank");
    }
    return repository.findById(id).orElseGet(() -> repository.save(User.withId(id)));
  }

  @Override
  public User save(User user) {
    if (user.getName() == null) {
      return repository.findById(user.getId()).orElseGet(() -> repository.save(user));
    }
    return repository.save(user);
  }

  @Override
  public void rememberName(String id, String name) {
    mongoTemplate.upsert(
        Query.query(Criteria.where("_id").is(id)), Update.update("name", name), User.class);
  }

  @Override
  public Optional<User> findByTelegramChatId(Long telegramChatId) {
    return repository.findByTelegramChatId(telegramChatId);
  }

  /**
   * Links a Telegram chat to a user, creating the user when it does not exist yet. A chat belongs
   * to a single user, so any other user previously linked to the same chat is unlinked first. The
   * name is only filled in when the user has none, so an existing name is never overwritten.
   *
   * @param userId JWT subject of the user to link
   * @param name display name to store when the user has none; may be null
   * @param telegramChatId Telegram chat identifier to link
   * @return the persisted, linked user
   * @throws IllegalArgumentException when the user identifier is blank or the chat id is null
   */
  @Override
  public User linkTelegramChat(String userId, String name, Long telegramChatId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User identifier cannot be blank");
    }
    if (telegramChatId == null) {
      throw new IllegalArgumentException("Telegram chat identifier cannot be null");
    }
    repository
        .findByTelegramChatId(telegramChatId)
        .filter(previous -> !previous.getId().equals(userId))
        .ifPresent(
            previous -> {
              previous.setTelegramChatId(null);
              repository.save(previous);
            });

    User user = repository.findById(userId).orElseGet(() -> User.withId(userId));
    if (user.getName() == null) {
      user.setName(name);
    }
    user.setTelegramChatId(telegramChatId);
    return repository.save(user);
  }
}
