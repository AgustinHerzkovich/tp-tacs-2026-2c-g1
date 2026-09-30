package com.solnotfound.service;

import com.solnotfound.dto.TelegramLinkCodeResponse;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.entity.user.TelegramLinkCode;
import com.solnotfound.exception.InvalidTelegramLinkCodeException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.mapper.UserMapper;
import com.solnotfound.repository.ITelegramLinkCodeRepository;
import com.solnotfound.repository.IUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private static final int LINK_CODE_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final IUserRepository userRepository;
  private final ITelegramLinkCodeRepository linkCodeRepository;
  private final Duration linkCodeTtl;
  private final Clock clock;

  @Autowired
  public UserService(
      IUserRepository userRepository,
      ITelegramLinkCodeRepository linkCodeRepository,
      @Value("${telegram.link-code.ttl:10m}") Duration linkCodeTtl) {
    this(userRepository, linkCodeRepository, linkCodeTtl, Clock.systemUTC());
  }

  UserService(
      IUserRepository userRepository,
      ITelegramLinkCodeRepository linkCodeRepository,
      Duration linkCodeTtl,
      Clock clock) {
    this.userRepository = userRepository;
    this.linkCodeRepository = linkCodeRepository;
    this.linkCodeTtl = linkCodeTtl;
    this.clock = clock;
  }

  /**
   * Resolves the user linked to a Telegram chat.
   *
   * @param telegramChatId Telegram chat identifier linked to the user
   * @return the linked user
   * @throws IllegalArgumentException when the chat identifier is missing
   * @throws ResourceNotFoundException when no user is linked to the chat
   */
  public UserDTO getUserByTelegramChatId(Long telegramChatId) {
    if (telegramChatId == null) {
      throw new IllegalArgumentException("Telegram chat identifier cannot be null");
    }
    System.Logger logger = System.getLogger(UserService.class.getName());
    logger.log(System.Logger.Level.INFO, "Resolving user for Telegram chat ID: " + telegramChatId);
    return userRepository
        .findByTelegramChatId(telegramChatId)
        .map(UserMapper::toDTO)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "No se encontró un usuario vinculado al chat de Telegram: " + telegramChatId));
  }

  /**
   * Issues a single-use code that lets whoever signs in with it link the given Telegram chat. The
   * bot puts the code in the login link instead of the chat id, so a link crafted by someone else
   * cannot attach the victim's account to the attacker's chat. Only the code hash is persisted.
   *
   * @param telegramChatId chat that requested the link, as reported by Telegram to the bot
   * @return the plain code and its expiration
   * @throws IllegalArgumentException when the chat identifier is missing
   */
  public TelegramLinkCodeResponse createTelegramLinkCode(Long telegramChatId) {
    if (telegramChatId == null) {
      throw new IllegalArgumentException("Telegram chat identifier cannot be null");
    }
    byte[] randomBytes = new byte[LINK_CODE_BYTES];
    RANDOM.nextBytes(randomBytes);
    String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    Instant expiresAt = clock.instant().plus(linkCodeTtl);
    linkCodeRepository.save(new TelegramLinkCode(hash(code), telegramChatId, expiresAt));
    return new TelegramLinkCodeResponse(code, expiresAt);
  }

  /**
   * Redeems a Telegram link code and links its chat to the authenticated user. A code can be
   * redeemed only once; if the chat was linked to another user, it moves to this one.
   *
   * @param userId JWT subject of the authenticated user
   * @param name display name from the token, stored only when the user has none
   * @param code single-use code received in the login link
   * @return the linked user
   * @throws InvalidTelegramLinkCodeException when the code is unknown, expired or already used
   */
  public UserDTO linkTelegramChat(String userId, String name, String code) {
    TelegramLinkCode linkCode =
        linkCodeRepository
            .consume(hash(code), clock.instant())
            .orElseThrow(
                () ->
                    new InvalidTelegramLinkCodeException(
                        "Telegram link code is unknown, expired or already used"));
    return UserMapper.toDTO(userRepository.linkTelegramChat(userId, name, linkCode.chatId()));
  }

  private static String hash(String code) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
