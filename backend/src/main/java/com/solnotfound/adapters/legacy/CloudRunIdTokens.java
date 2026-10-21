package com.solnotfound.adapters.legacy;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Identity tokens for calling the legacy service when it runs as a private Cloud Run service. The
 * service only accepts callers with the invoker role, so each call carries an identity token of the
 * backend's service account for the service's URL. It goes in {@code X-Serverless-Authorization}
 * because {@code Authorization} already carries the legacy API token. The library caches the token
 * and renews it before it expires.
 */
public final class CloudRunIdTokens implements Supplier<String> {

  private final IdTokenCredentials credentials;

  private CloudRunIdTokens(IdTokenCredentials credentials) {
    this.credentials = credentials;
  }

  /** Uses the application default credentials; fails when there are none. */
  public static CloudRunIdTokens forAudience(String audience) {
    try {
      GoogleCredentials source = GoogleCredentials.getApplicationDefault();
      if (!(source instanceof IdTokenProvider provider)) {
        throw new IllegalStateException("Credentials cannot issue identity tokens");
      }
      return new CloudRunIdTokens(
          IdTokenCredentials.newBuilder()
              .setIdTokenProvider(provider)
              .setTargetAudience(audience)
              .build());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "No Google credentials to call the legacy service", exception);
    }
  }

  @Override
  public String get() {
    try {
      credentials.refreshIfExpired();
      return credentials.getIdToken().getTokenValue();
    } catch (IOException exception) {
      throw new LegacyProviderException(
          LegacyFailure.UNREACHABLE,
          "Could not get an identity token for the legacy service",
          exception);
    }
  }
}
