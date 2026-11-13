package com.solnotfound.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

  /**
   * The OpenTelemetry instance the manual instrumentation uses. With the Java agent attached (local
   * Docker Compose) it is the agent's SDK, so manual and automatic spans share the same traces and
   * exporter; without the agent (tests, Cloud Run) it is a no-op and records nothing.
   */
  @Bean
  public OpenTelemetry openTelemetry() {
    return GlobalOpenTelemetry.get();
  }
}
