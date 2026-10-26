package com.solnotfound.adapters.legacy;

import java.util.function.Predicate;

/**
 * Tells the circuit breaker which failures are the provider's fault. A request the provider rejects
 * for being invalid does not count against its health; every other legacy failure does. Referenced
 * by name from {@code application.properties}.
 */
public class LegacyProviderFaultPredicate implements Predicate<Throwable> {

  @Override
  public boolean test(Throwable throwable) {
    return throwable instanceof LegacyProviderException exception && exception.isProviderFault();
  }
}
