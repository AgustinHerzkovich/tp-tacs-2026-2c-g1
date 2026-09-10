package com.solnotfound.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableCaching
@EnableConfigurationProperties(WeatherCacheProperties.class)
public class WeatherConfig {

  @Bean
  RestClient.Builder weatherRestClientBuilder(
      @Value("${weather.connect-timeout:2s}") Duration connectTimeout,
      @Value("${weather.read-timeout:4s}") Duration readTimeout) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_2)
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder().requestFactory(requestFactory);
  }

  /**
   * Creates bounded caches with TTLs matching provider update frequency and usage characteristics.
   * No expired weather data is served when Open-Meteo is unavailable.
   *
   * @return cache manager for coordinates, current conditions, and forecast ranges
   */
  @Bean
  CacheManager weatherCacheManager(WeatherCacheProperties properties) {
    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(
        List.of(
            cache("weather-geocoding", properties.geocoding()),
            cache("weather-current", properties.current()),
            cache("weather-forecast", properties.forecast())));
    return manager;
  }

  private CaffeineCache cache(String name, WeatherCacheProperties.CacheSpec properties) {
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .expireAfterWrite(properties.ttl())
            .maximumSize(properties.maximumSize())
            .build());
  }
}
