package com.solnotfound.adapters.legacy;

import java.util.function.Predicate;

/**
 * Tells the retry which failures are worth another attempt: the transient ones, such as a hung call
 * or a broken response. A rejected request or an unreachable service are not retried. Referenced by
 * name from {@code application.properties}.
 */
public class LegacyRetryPredicate implements Predicate<Throwable> {

  @Override
  public boolean test(Throwable throwable) {
    return throwable instanceof LegacyProviderException exception && exception.isRetryable();
  }
}
