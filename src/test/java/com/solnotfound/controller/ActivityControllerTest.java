package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.*;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.repository.ActivityMockRepository;
import com.solnotfound.service.ActivityService;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ActivityControllerTest {

  @Test
  void respondsCreatedWithResourceLocation() {
    ActivityController controller =
        new ActivityController(new ActivityService(new ActivityMockRepository()));
    LocationDTO location = new LocationDTO("Buenos Aires", null, null);
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            location,
            LocalDateTime.now().plusDays(1),
            10,
            20,
            new WeatherConditionsDTO(30, 10, 28, 25.0),
            15,
            new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));

    ResponseEntity<ActivityResponse> response = controller.create(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getHeaders().getLocation())
        .isEqualTo(URI.create("/activities/" + response.getBody().id()));
  }
}
