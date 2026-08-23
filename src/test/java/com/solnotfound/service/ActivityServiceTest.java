package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.exception.InvalidActivityException;
import com.solnotfound.repository.ActivityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {

  private ActivityService activityService;

  @BeforeEach
  void setUp() {
    activityService = new ActivityService(new ActivityRepository());
  }

  @Test
  void createsActivityAndMapsWeatherConditions() {
    ActivityResponse created = activityService.create(validRequest());

    assertThat(created.id()).isNotBlank();
    assertThat(created.title()).isEqualTo("Football match");
    assertThat(created.weatherConditions()).isEqualTo(new WeatherConditionsDTO(30, 10, 28, 25.0));
    assertThat(activityService.getById(created.id())).isEqualTo(created);
  }

  @Test
  void rejectsParticipantRangeWhenMinimumExceedsMaximum() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            20,
            10,
            validWeatherConditions(),
            15);

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Minimum participants cannot exceed maximum participants");
  }

  @Test
  void rejectsIncompleteTemperatureRange() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            new WeatherConditionsDTO(30, 10, null, 25.0),
            3);

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Minimum and maximum temperature must be provided together");
  }

  @Test
  void rejectsInvertedTemperatureRange() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            new WeatherConditionsDTO(30, 28, 10, 25.0),
            4);

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Minimum temperature cannot exceed maximum temperature");
  }

  @Test
  void rejectsLocationWithoutCityOrCoordinates() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            new LocationDTO(null, null, null),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            validWeatherConditions(),
            3);

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Location must contain a city or coordinates");
  }

  @Test
  void rejectsIncompleteCoordinates() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            new LocationDTO(null, -34.6, null),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            validWeatherConditions(),
            2);

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Latitude and longitude must be provided together");
  }

  private CreateActivityRequest validRequest() {
    return new CreateActivityRequest(
        "Football match",
        "Friendly match",
        ActivityType.OUTDOOR,
        cityLocation(),
        LocalDateTime.now().plusDays(1),
        10,
        20,
        validWeatherConditions(),
        4);
  }

  private LocationDTO cityLocation() {
    return new LocationDTO("Buenos Aires", null, null);
  }

  private WeatherConditionsDTO validWeatherConditions() {
    return new WeatherConditionsDTO(30, 10, 28, 25.0);
  }
}
