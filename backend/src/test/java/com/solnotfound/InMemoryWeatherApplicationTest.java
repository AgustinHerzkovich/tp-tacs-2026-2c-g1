package com.solnotfound;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.adapters.InMemoryWeatherAdapter;
import com.solnotfound.adapters.openmeteo.OpenMeteoClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class InMemoryWeatherApplicationTest {

  @Test
  void contextUsesOnlyInMemoryWeatherComponents() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource("test", java.util.Map.of("weather.provider", "in-memory")));
      context.register(InMemoryWeatherAdapter.class, OpenMeteoClient.class);
      context.refresh();

      assertThat(context.getBean(IWeatherAdapter.class)).isInstanceOf(InMemoryWeatherAdapter.class);
      assertThat(context.getBeansOfType(OpenMeteoClient.class)).isEmpty();
    }
  }
}
