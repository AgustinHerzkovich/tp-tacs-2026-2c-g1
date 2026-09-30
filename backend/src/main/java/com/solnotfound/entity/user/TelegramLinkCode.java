package com.solnotfound.entity.user;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A pending, single-use code that links a Telegram chat to the user who redeems it.
 *
 * <p>Only the SHA-256 hash of the code is stored, so a database leak does not expose usable codes.
 * The TTL index removes expired codes; redeeming also checks {@code expiresAt} because MongoDB
 * deletes expired documents lazily.
 *
 * @param codeHash hex SHA-256 hash of the code sent to the user
 * @param chatId Telegram chat that requested the code
 * @param expiresAt instant after which the code can no longer be redeemed
 */
@Document(collection = "telegram_link_codes")
public record TelegramLinkCode(
    @Id String codeHash, Long chatId, @Indexed(expireAfter = "0s") Instant expiresAt) {}
