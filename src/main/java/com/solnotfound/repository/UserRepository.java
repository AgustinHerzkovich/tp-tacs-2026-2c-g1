package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository implements IUserRepository {
  private final MongoUserRepository repository;

  public UserRepository(MongoUserRepository repository) {
    this.repository = repository;
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
}
