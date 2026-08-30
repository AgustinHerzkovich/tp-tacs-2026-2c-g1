package com.solnotfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.GlobalExceptionHandler;
import com.solnotfound.service.VotationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VotationControllerTest {

  private VotationService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(VotationService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new VotationController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void listsCurrentUsersVotations() throws Exception {
    when(service.getByOrganizerOrParticipantId("user-1")).thenReturn(List.of());

    mockMvc
        .perform(get("/votations/me").principal(authentication("user-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());

    verify(service).getByOrganizerOrParticipantId("user-1");
  }

  @Test
  void organizerUpdatesVotationOptions() throws Exception {
    when(service.updateVotationOptions(eq("v-1"), any(), eq("organizer")))
        .thenReturn(
            new VotationDTO(
                "v-1", "activity-1", LocalDateTime.now(), VotationStatus.ACTIVE, List.of()));

    mockMvc
        .perform(
            put("/votations/v-1/options")
                .principal(authentication("organizer"))
                .contentType("application/json")
                .content("{\"dates\":[\"2026-09-01T12:00:00\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("v-1"))
        .andExpect(jsonPath("$.activityId").value("activity-1"));

    verify(service).updateVotationOptions(eq("v-1"), any(), eq("organizer"));
  }

  @Test
  void rejectsOptionUpdateByNonOrganizer() throws Exception {
    when(service.updateVotationOptions(eq("v-1"), any(), eq("participant")))
        .thenThrow(new AccessDeniedException("Only the activity organizer can update options"));

    mockMvc
        .perform(
            put("/votations/v-1/options")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("{\"dates\":[\"2026-09-01T12:00:00\"]}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access denied"));
  }

  @Test
  void rejectsEmptyOptionsRequest() throws Exception {
    mockMvc
        .perform(
            put("/votations/v-1/options")
                .principal(authentication("organizer"))
                .contentType("application/json")
                .content("{\"dates\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors.dates").exists());
  }

  private TestingAuthenticationToken authentication(String userId) {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId, null);
    authentication.setAuthenticated(true);
    return authentication;
  }
}
