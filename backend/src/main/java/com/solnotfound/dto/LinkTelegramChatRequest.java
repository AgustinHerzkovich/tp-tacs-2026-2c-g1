package com.solnotfound.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to link the authenticated user to a Telegram chat.
 *
 * @param code single-use link code issued by the bot; the chat is never taken from the client
 */
public record LinkTelegramChatRequest(@NotBlank String code) {}
