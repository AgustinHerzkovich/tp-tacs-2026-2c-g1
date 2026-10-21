package com.solnotfound.config;

import com.solnotfound.adapters.legacy.LegacyWeatherClient;
import com.solnotfound.adapters.legacy.LegacyWeatherProperties;
import com.solnotfound.adapters.legacy.LegacyWeatherProvider;
import com.solnotfound.service.StatisticsEventRecorder;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(LegacyWeatherProperties.class)
public class LegacyWeatherConfig {

  static final String INSTANCE = "legacyWeather";

  @Bean
  LegacyWeatherClient legacyWeatherClient(LegacyWeatherProperties properties) {
    // A dedicated, strict mapper: trailing commas, unquoted values or trailing garbage are errors.
    return new LegacyWeatherClient(properties, JsonMapper.builder().build());
  }

  @Bean
  LegacyWeatherProvider legacyWeatherProvider(
      LegacyWeatherClient client,
      CircuitBreakerRegistry circuitBreakers,
      RetryRegistry retries,
      RateLimiterRegistry rateLimiters,
      BulkheadRegistry bulkheads,
      StatisticsEventRecorder statisticsRecorder,
      LegacyWeatherProperties properties) {
    return new LegacyWeatherProvider(
        client,
        circuitBreakers.circuitBreaker(INSTANCE),
        retries.retry(INSTANCE),
        rateLimiters.rateLimiter(INSTANCE),
        bulkheads.bulkhead(INSTANCE),
        statisticsRecorder,
        properties);
  }
}
