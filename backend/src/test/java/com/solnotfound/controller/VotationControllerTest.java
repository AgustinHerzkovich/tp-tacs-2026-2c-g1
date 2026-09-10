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
import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.GlobalExceptionHandler;
import com.solnotfound.exception.ResourceNotFoundException;
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
        .perform(get("/votations").principal(authentication("user-1")))
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

  @Test
  void participantVotesAndReceivesPartialResult() throws Exception {
    LocalDateTime option = LocalDateTime.of(2026, 9, 1, 12, 0);
    when(service.vote("v-1", "participant", option))
        .thenReturn(
            new VotationDTO(
                "v-1",
                "activity-1",
                LocalDateTime.now(),
                VotationStatus.ACTIVE,
                List.of(new VotationOptionDTO(option, 1, List.of("Jane Doe")))));

    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("\"2026-09-01T12:00:00\""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.options[0].voteCount").value(1))
        .andExpect(jsonPath("$.options[0].voterNames[0]").value("Jane Doe"));

    verify(service).vote("v-1", "participant", option);
  }

  @Test
  void rejectsVoteOnUnknownVotation() throws Exception {
    LocalDateTime option = LocalDateTime.of(2026, 9, 1, 12, 0);
    when(service.vote("v-1", "participant", option))
        .thenThrow(new ResourceNotFoundException("Votation not found: v-1"));

    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("\"2026-09-01T12:00:00\""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }

  @Test
  void rejectsVoteOnUnknownOption() throws Exception {
    LocalDateTime option = LocalDateTime.of(2026, 9, 1, 12, 0);
    when(service.vote("v-1", "participant", option))
        .thenThrow(new ResourceNotFoundException("Option not found: " + option));

    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("\"2026-09-01T12:00:00\""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }

  @Test
  void rejectsVoteByUserOutsideActivity() throws Exception {
    LocalDateTime option = LocalDateTime.of(2026, 9, 1, 12, 0);
    when(service.vote("v-1", "outsider", option))
        .thenThrow(new ResourceNotFoundException("User doesn't belong to this activity: outsider"));

    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("outsider"))
                .contentType("application/json")
                .content("\"2026-09-01T12:00:00\""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }

  @Test
  void rejectsVoteOnClosedVotation() throws Exception {
    LocalDateTime option = LocalDateTime.of(2026, 9, 1, 12, 0);
    when(service.vote("v-1", "participant", option))
        .thenThrow(new AccessDeniedException("Votation already closed: v-1"));

    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("\"2026-09-01T12:00:00\""))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access denied"));
  }

  @Test
  void rejectsMalformedVoteBody() throws Exception {
    mockMvc
        .perform(
            put("/votations/v-1/votes/me")
                .principal(authentication("participant"))
                .contentType("application/json")
                .content("\"not-a-date\""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void organizerUpdatesVotationSettings() throws Exception {
    when(service.updateVotationSettings(eq("v-1"), any(), eq("organizer")))
        .thenReturn(
            new VotationDTO(
                "v-1", "activity-1", LocalDateTime.now(), VotationStatus.ACTIVE, List.of()));

    mockMvc
        .perform(
            put("/votations/v-1/settings")
                .principal(authentication("organizer"))
                .contentType("application/json")
                .content("{\"minQuorum\":0.75,\"duration\":\"PT2H\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("v-1"));

    verify(service).updateVotationSettings(eq("v-1"), any(), eq("organizer"));
  }

  @Test
  void rejectsOutOfRangeQuorum() throws Exception {
    mockMvc
        .perform(
            put("/votations/v-1/settings")
                .principal(authentication("organizer"))
                .contentType("application/json")
                .content("{\"minQuorum\":1.1,\"duration\":\"PT2H\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.minQuorum").exists());
  }

  private TestingAuthenticationToken authentication(String userId) {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId, null);
    authentication.setAuthenticated(true);
    return authentication;
  }
}
