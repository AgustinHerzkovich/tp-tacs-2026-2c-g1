package com.solnotfound.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.cache")
public record WeatherCacheProperties(CacheSpec geocoding, CacheSpec current, CacheSpec forecast) {

  public record CacheSpec(Duration ttl, long maximumSize) {}
}
