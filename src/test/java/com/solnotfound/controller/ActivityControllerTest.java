package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ParticipantDTO;
import com.solnotfound.dto.ParticipantRequest;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.repository.ActivityRepository;
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
        new ActivityController(new ActivityService(new ActivityRepository()));
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

  @Test
  void joinsParticipantAndReturnsOk() {
    ActivityController controller =
        new ActivityController(new ActivityService(new ActivityRepository()));

    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            new LocationDTO("Buenos Aires", null, null),
            LocalDateTime.now().plusDays(1),
            1,
            10,
            new WeatherConditionsDTO(30, 10, 28, 25.0),
            15,
            new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));

    ActivityResponse activity = controller.create(request).getBody();

    ResponseEntity<ActivityResponse> response =
        controller.join(activity.id(), new ParticipantRequest("user-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(response.getBody()).isNotNull();

    assertThat(response.getBody().participantCount()).isEqualTo(1);

    assertThat(response.getBody().participants())
        .extracting(ParticipantDTO::userId)
        .containsExactly("user-1");
  }

  @Test
  void removesParticipantAndReturnsOk() {
    ActivityController controller =
        new ActivityController(new ActivityService(new ActivityRepository()));

    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            new LocationDTO("Buenos Aires", null, null),
            LocalDateTime.now().plusDays(1),
            1,
            10,
            new WeatherConditionsDTO(30, 10, 28, 25.0),
            15,
            new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));

    ActivityResponse activity = controller.create(request).getBody();

    controller.join(activity.id(), new ParticipantRequest("user-1"));

    ResponseEntity<ActivityResponse> response = controller.leave(activity.id(), "user-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(response.getBody()).isNotNull();

    assertThat(response.getBody().participantCount()).isZero();

    assertThat(response.getBody().participants()).isEmpty();
  }
}
