package com.solnotfound.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.exception.GlobalExceptionHandler;
import com.solnotfound.repository.ActivityMockRepository;
import com.solnotfound.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivitySearchControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ActivityController controller =
        new ActivityController(new ActivityService(new ActivityMockRepository()));
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
        .andExpect(jsonPath("$.detail").value("Search start date cannot be after end date"));
  }

  @Test
  void returnsProblemDetailForInvalidSearchParameter() throws Exception {
    mockMvc
        .perform(get("/activities").param("type", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request parameter"))
        .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'type'"))
        .andExpect(jsonPath("$.parameter").value("type"));
  }
}
