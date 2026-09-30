package com.solnotfound.repository;

import com.solnotfound.entity.user.TelegramLinkCode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTelegramLinkCodeRepository implements ITelegramLinkCodeRepository {
  private final Map<String, TelegramLinkCode> codes = new ConcurrentHashMap<>();

  @Override
  public void save(TelegramLinkCode linkCode) {
    codes.put(linkCode.codeHash(), linkCode);
  }

  @Override
  public Optional<TelegramLinkCode> consume(String codeHash, Instant now) {
    return Optional.ofNullable(codes.remove(codeHash))
        .filter(linkCode -> linkCode.expiresAt().isAfter(now));
  }

  public java.util.Collection<TelegramLinkCode> stored() {
    return java.util.List.copyOf(codes.values());
  }
}
