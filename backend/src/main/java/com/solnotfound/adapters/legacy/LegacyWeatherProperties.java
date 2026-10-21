package com.solnotfound.adapters.legacy;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings of the legacy weather service. The resilience settings (retry, circuit
 * breaker, rate limiter and bulkhead) live under {@code resilience4j.*.instances.legacyWeather}.
 *
 * @param apiToken token printed by the service after signing in; never versioned
 * @param apiTokenFile file holding the token, written by the legacy container after signing in;
 *     used when {@code apiToken} is blank
 * @param readTimeout maximum time to wait for a response; the service sometimes never answers
 * @param userRequestsPerMinute weather lookups a single user can send to the service per minute
 * @param cloudRunAudience URL of the legacy Cloud Run service when it is private; blank locally
 */
@ConfigurationProperties(prefix = "weather.legacy")
public record LegacyWeatherProperties(
    URI baseUrl,
    String apiToken,
    String apiTokenFile,
    Duration connectTimeout,
    Duration readTimeout,
    Duration cacheTtl,
    long cacheMaximumSize,
    int userRequestsPerMinute,
    String cloudRunAudience) {}
