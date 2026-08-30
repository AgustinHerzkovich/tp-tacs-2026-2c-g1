package com.solnotfound.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.ActivityStatus;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.service.ActivityService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityParticipationControllerTest {

  @Autowired private ActivityRepository repository;
  @Autowired private ActivityService service;
  @Autowired private MockMvc mockMvc;

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
