package com.solnotfound.dto;

import java.time.Instant;

/**
 * A freshly issued Telegram link code, to be sent to the chat inside the login link.
 *
 * @param code single-use code; it is only returned here and never stored in plain text
 * @param expiresAt instant after which the code can no longer be redeemed
 */
public record TelegramLinkCodeResponse(String code, Instant expiresAt) {}
