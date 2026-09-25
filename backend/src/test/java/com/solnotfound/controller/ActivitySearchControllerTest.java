package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.PageResponse;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.exception.GlobalExceptionHandler;
import com.solnotfound.repository.InMemoryActivityRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import com.solnotfound.service.ActivityService;
import com.solnotfound.service.StatisticsEventRecorder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivitySearchControllerTest {

  private MockMvc mockMvc;
  private IWeatherAdapter weatherAdapter;

  @BeforeEach
  void setUp() {
    weatherAdapter = org.mockito.Mockito.mock(IWeatherAdapter.class);

    ActivityController controller =
        new ActivityController(
            new ActivityService(
                new InMemoryActivityRepository(),
                weatherAdapter,
                new InMemoryUserRepository(),
                org.mockito.Mockito.mock(StatisticsEventRecorder.class)));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void rejectsInvertedDateRange() throws Exception {
    mockMvc
        .perform(
            get("/activities")
                .param("dateFrom", "2026-09-10T00:00:00")
                .param("dateTo", "2026-09-01T00:00:00"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid activity"))
        .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"))
        .andExpect(jsonPath("$.detail").value("Search start date cannot be after end date"));
  }

  @Test
  void returnsProblemDetailForInvalidSearchParameter() throws Exception {
    mockMvc
        .perform(get("/activities").param("type", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request parameter"))
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
        .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'type'"))
        .andExpect(jsonPath("$.parameter").value("type"));
  }

  /**
   * Explorar defaults to a comma-separated {@code status} query param (e.g. {@code
   * status=CONFIRMED,PROPOSED,RESCHEDULED}) to hide finished/cancelled activities by default. This
   * locks in that Spring binds it into every requested {@link ActivityStatus}, not just the first
   * one.
   */
  @Test
  void bindsACommaSeparatedStatusParameterIntoEveryRequestedStatus() throws Exception {
    ActivityService serviceMock = Mockito.mock(ActivityService.class);
    Mockito.when(serviceMock.search(Mockito.any(), Mockito.any()))
        .thenReturn(PageResponse.from(new PageImpl<>(List.of())));
    MockMvc mockedServiceMvc =
        MockMvcBuilders.standaloneSetup(new ActivityController(serviceMock))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    mockedServiceMvc
        .perform(get("/activities").param("status", "CONFIRMED,PROPOSED,RESCHEDULED"))
        .andExpect(status().isOk());

    ArgumentCaptor<ActivityFilterDTO> filterCaptor =
        ArgumentCaptor.forClass(ActivityFilterDTO.class);
    verify(serviceMock).search(filterCaptor.capture(), any(Pageable.class));
    assertThat(filterCaptor.getValue().statuses())
        .containsExactly(
            ActivityStatus.CONFIRMED, ActivityStatus.PROPOSED, ActivityStatus.RESCHEDULED);
  }
}
