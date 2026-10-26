package com.solnotfound.repository;

import com.solnotfound.entity.user.TelegramLinkCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramLinkCodeRepository implements ITelegramLinkCodeRepository {
  private final MongoTemplate mongoTemplate;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared MongoTemplate bean")
  public TelegramLinkCodeRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void save(TelegramLinkCode linkCode) {
    mongoTemplate.save(linkCode);
  }

  @Override
  public Optional<TelegramLinkCode> consume(String codeHash, Instant now) {
    Query query = Query.query(Criteria.where("_id").is(codeHash).and("expiresAt").gt(now));
    return Optional.ofNullable(mongoTemplate.findAndRemove(query, TelegramLinkCode.class));
  }
}
