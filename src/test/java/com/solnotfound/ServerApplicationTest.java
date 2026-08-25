package com.solnotfound;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.INotificationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class ServerApplicationTest {

  @MockitoBean private IWeatherAdapter weatherAdapter;

  @MockitoBean private IBadWeatherChecker badWeatherChecker;

  @MockitoBean private INotificationFacade notificationFacade;

  @Test
  void contextLoads() {}
}
