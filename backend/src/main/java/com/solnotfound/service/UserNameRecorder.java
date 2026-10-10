package com.solnotfound.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.solnotfound.repository.IUserRepository;
import java.time.Duration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Keeps the display name of each user in sync with the one in their access token, so other people
 * see a real name instead of an identifier.
 *
 * <p>The identity provider owns the name and the application only learns it from the token of an
 * authenticated request. Recording it on every visit also fills in the users created before names
 * were stored, and picks up a later rename, without any migration. A small cache remembers the
 * names already written by this instance, so a returning user costs no database write.
 */
@Service
public class UserNameRecorder {

  private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

  private final IUserRepository userRepository;
  private final Cache<String, String> recordedNames =
      Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofHours(1)).build();

  public UserNameRecorder(IUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Stores the name carried by a verified token for its subject, creating the user when needed.
   * Tokens of machine clients (service accounts) and tokens without a usable name are ignored.
   *
   * @param jwt verified access token of the current request
   */
  public void record(Jwt jwt) {
    String userId = jwt.getSubject();
    String name = displayName(jwt);
    if (userId == null || userId.isBlank() || name == null || name.isBlank()) {
      return;
    }
    String username = jwt.getClaimAsString("preferred_username");
    if (username != null && username.startsWith(SERVICE_ACCOUNT_PREFIX)) {
      return;
    }
    if (name.equals(recordedNames.getIfPresent(userId))) {
      return;
    }
    userRepository.rememberName(userId, name);
    recordedNames.put(userId, name);
  }

  /**
   * Resolves the name to show for the owner of a token: the full name when the token has one, the
   * username otherwise.
   *
   * @param jwt verified access token
   * @return the display name, or {@code null} when the token carries neither claim
   */
  public static String displayName(Jwt jwt) {
    String name = jwt.getClaimAsString("name");
    return name != null && !name.isBlank() ? name : jwt.getClaimAsString("preferred_username");
  }
}
