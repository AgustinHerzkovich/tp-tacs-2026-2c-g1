package com.solnotfound.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solnotfound.repository.IStatisticsEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Same context configuration as StatisticsSecurityTest so both share one cached context.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class ForwardedHeadersTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IStatisticsEventRepository repository;

  @Test
  void resourceMetadataLinkUsesTheOriginSeenByTheProxy() throws Exception {
    mockMvc
        .perform(
            get("/activities")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "planazo.example")
                .header("X-Forwarded-Port", "443"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header()
                .string(
                    HttpHeaders.WWW_AUTHENTICATE,
                    containsString(
                        "resource_metadata=\"https://planazo.example/.well-known/oauth-protected-resource\"")));
  }
}
