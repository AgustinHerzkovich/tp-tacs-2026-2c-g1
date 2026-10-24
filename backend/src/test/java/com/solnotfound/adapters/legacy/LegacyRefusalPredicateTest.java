package com.solnotfound.adapters.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class LegacyRefusalPredicateTest {

  private final LegacyRefusalPredicate predicate = new LegacyRefusalPredicate();

  @Test
  void ignoresLookupsTurnedAwayByTheBulkheadOrTheRateLimiter() {
    assertThat(
            predicate.test(
                BulkheadFullException.createBulkheadFullException(Bulkhead.ofDefaults("legacy"))))
        .isTrue();
    assertThat(
            predicate.test(
                RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("legacy"))))
        .isTrue();
  }

  @Test
  void doesNotIgnoreFailuresOfTheService() {
    assertThat(predicate.test(new IOException("connection refused"))).isFalse();
    assertThat(predicate.test(new LegacyProviderUnavailableException("TIMEOUT"))).isFalse();
  }
}
