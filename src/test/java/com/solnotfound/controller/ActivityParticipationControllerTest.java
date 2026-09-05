package com.solnotfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.repository.IStatisticsEventRepository;
import com.solnotfound.service.ActivityService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityParticipationControllerTest {

  @Autowired private ActivityRepository repository;
  @Autowired private ActivityService service;
  @Autowired private MockMvc mockMvc;
  @MockitoBean private IWeatherAdapter weatherAdapter;
  @MockitoBean private IStatisticsEventRepository statisticsEventRepository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  void publicEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/activities")).andExpect(status().isOk());
  }

  @Test
  void joinAndLeaveUseJwtSubject() throws Exception {
    String activityId = service.create(request(1, 2)).id();

    mockMvc
        .perform(
            put("/activities/{id}/participants/me", activityId)
                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants[0].userId").value("user-1"));

    mockMvc
        .perform(
            delete("/activities/{id}/participants/me", activityId)
                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(0));
  }

  @Test
  void repeatedJoinAndLeaveAreIdempotent() throws Exception {
    String activityId = service.create(request(1, 2)).id();

    mockMvc.perform(put("/activities/{id}/participants/me", activityId)).andExpect(status().isOk());
    mockMvc
        .perform(put("/activities/{id}/participants/me", activityId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(1));

    mockMvc
        .perform(delete("/activities/{id}/participants/me", activityId))
        .andExpect(status().isOk());
    mockMvc
        .perform(delete("/activities/{id}/participants/me", activityId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(0));
  }

  @Test
  void returnsConflictWhenActivityCannotAcceptParticipants() throws Exception {
    String activityId = service.create(request(1, 2)).id();
    repository.findById(activityId).setStatus(ActivityStatus.CANCELLED);

    mockMvc
        .perform(put("/activities/{id}/participants/me", activityId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Activity state conflict"));
  }

  @Test
  void weatherUsesJwtSubjectAndSerializesLocation() throws Exception {
    var activity = service.create(request(1, 2));
    service.join(activity.id(), "user-1");
    WeatherForecast current = new WeatherForecast(LocalDateTime.now(), 22.0f, 10.0f, 15.0f);
    WeatherForecast forecast = new WeatherForecast(activity.dateTime(), 18.0f, 60.0f, 30.0f);
    when(weatherAdapter.getWeather(any(Location.class))).thenReturn(current);
    when(weatherAdapter.getFutureClimate(any(Location.class), eq(activity.dateTime())))
        .thenReturn(forecast);

    mockMvc
        .perform(
            get("/activities/{id}/weather", activity.id())
                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location.city").value("Buenos Aires"))
        .andExpect(jsonPath("$.locationDTO").doesNotExist())
        .andExpect(jsonPath("$.currentWeather.temperature").value(22.0));
  }

  @Test
  void weatherReturnsForbiddenOrNotFoundAsProblemDetail() throws Exception {
    String activityId = service.create(request(1, 2)).id();

    mockMvc
        .perform(
            get("/activities/{id}/weather", activityId)
                .with(jwt().jwt(jwt -> jwt.subject("outsider"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Activity access denied"));

    mockMvc
        .perform(
            get("/activities/{id}/weather", "missing")
                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Activity not found"));
  }

  private CreateActivityRequest request(int minParticipants, int maxParticipants) {
    return new CreateActivityRequest(
        "Football match",
        "Friendly match",
        ActivityType.OUTDOOR,
        new LocationDTO("Buenos Aires", null, null),
        LocalDateTime.now().plusDays(1),
        minParticipants,
        maxParticipants,
        new WeatherConditionsDTO(30, 10, 28, 25.0),
        4,
        new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
  }
}
