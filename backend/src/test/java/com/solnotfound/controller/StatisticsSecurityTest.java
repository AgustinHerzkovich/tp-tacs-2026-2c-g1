package com.solnotfound.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.repository.IStatisticsEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// This test only exercises security and runs without MongoDB (also in the Docker image build), so
// the startup index creation, which needs a live server, is disabled.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class StatisticsSecurityTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IStatisticsEventRepository repository;

  @Test
  void statisticsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/statistics")).andExpect(status().isUnauthorized());
  }

  @Test
  void statisticsRejectAuthenticatedUsersWithoutAdminRole() throws Exception {
    mockMvc
        .perform(
            get("/statistics").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void statisticsAllowAdminUsers() throws Exception {
    mockMvc
        .perform(
            get("/statistics").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }
}
