package com.solnotfound.exception;

/**
 * Signals that a change could not be applied because the same data kept being modified by other
 * requests while it was retried. Nothing was stored; repeating the request is safe.
 */
public class ConcurrentUpdateException extends CodedException {

  public ConcurrentUpdateException(String message) {
    super(ErrorCode.CONCURRENT_UPDATE, message);
  }
}
