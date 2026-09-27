package com.solnotfound.dto;

import jakarta.validation.constraints.NotNull;

public record LinkTelegramChatRequest(@NotNull Long chatId) {}
