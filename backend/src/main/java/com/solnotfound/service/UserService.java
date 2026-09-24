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
    return userRepository
        .findByTelegramChatId(telegramChatId)
        .map(UserMapper::toDTO)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "No se encontró un usuario vinculado al chat de Telegram: " + telegramChatId));
  }
}
