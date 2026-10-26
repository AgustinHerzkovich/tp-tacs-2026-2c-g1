package com.solnotfound.repository;

import com.solnotfound.entity.user.TelegramLinkCode;
import java.time.Instant;
import java.util.Optional;

public interface ITelegramLinkCodeRepository {

  void save(TelegramLinkCode linkCode);

  /**
   * Atomically removes and returns the code with the given hash if it has not expired, so a code
   * can be redeemed at most once even under concurrent requests.
   *
   * @param codeHash hex SHA-256 hash of the code
   * @param now instant used to discard expired codes
   * @return the redeemed code, or empty when it does not exist, was already used or expired
   */
  Optional<TelegramLinkCode> consume(String codeHash, Instant now);
}
