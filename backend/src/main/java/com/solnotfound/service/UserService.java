package com.solnotfound.service;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.mapper.UserMapper;
import com.solnotfound.repository.IUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final IUserRepository userRepository;

  public UserService(IUserRepository userRepository) {
    this.userRepository = userRepository;
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
   * Links the Telegram chat that opened the login link to the authenticated user. If the chat was
   * linked to another user, it moves to this one.
   *
   * @param userId JWT subject of the authenticated user
   * @param name display name from the token, stored only when the user has none
   * @param telegramChatId Telegram chat identifier received in the login link
   * @return the linked user
   * @throws IllegalArgumentException when the chat identifier is missing
   */
  public UserDTO linkTelegramChat(String userId, String name, Long telegramChatId) {
    if (telegramChatId == null) {
      throw new IllegalArgumentException("Telegram chat identifier cannot be null");
    }
    return UserMapper.toDTO(userRepository.linkTelegramChat(userId, name, telegramChatId));
  }
}
