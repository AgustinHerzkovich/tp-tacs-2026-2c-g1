package com.solnotfound;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.weather.IBadWeatherChecker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(
    properties = "spring.task.scheduling.enabled=false",
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerApplicationTest {

  @MockitoBean private IWeatherAdapter weatherAdapter;

  @MockitoBean private IBadWeatherChecker badWeatherChecker;

  @TestConfiguration(proxyBeanMethods = false)
  static class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
      return new MongoDBContainer("mongo:latest");
    }
  }

  @Test
  void contextLoads() {}
}
