package com.solnotfound.adapters.legacy;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.function.Predicate;

/**
 * Tells the circuit breaker to ignore the calls that the platform's own protections turned away
 * before they reached the service. Without it they would count as successes: while the service
 * hangs, the bulkhead turns away most lookups, and those "successes" would keep the failure rate
 * under the threshold so the circuit never opens. Referenced by name from {@code
 * application.properties}.
 */
public class LegacyRefusalPredicate implements Predicate<Throwable> {

  @Override
  public boolean test(Throwable throwable) {
    return throwable instanceof BulkheadFullException || throwable instanceof RequestNotPermitted;
  }
}
