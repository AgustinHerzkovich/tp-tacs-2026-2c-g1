package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.ActivityWeatherResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ParticipantDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.repository.InMemoryActivityRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import com.solnotfound.service.ActivityService;
import com.solnotfound.service.StatisticsEventRecorder;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityControllerTest {

  @Test
  void respondsCreatedWithResourceLocation() {
    IWeatherAdapter weatherAdapter = mock(IWeatherAdapter.class);
    ActivityController controller =
        new ActivityController(
            new ActivityService(
                new InMemoryActivityRepository(),
                weatherAdapter,
                new InMemoryUserRepository(),
                mock(StatisticsEventRecorder.class)));
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

    ResponseEntity<ActivityResponse> response =
        controller.create(request, List.of(), authentication("creator-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getHeaders().getLocation())
        .isEqualTo(URI.create("/activities/" + response.getBody().id()));
  }

  @Test
  void acceptsMultipartCreationWithoutImages() throws Exception {
    ActivityService service = mock(ActivityService.class);
    ActivityResponse response = mock(ActivityResponse.class);
    when(response.id()).thenReturn("activity-1");
    when(service.create(any(), eq("creator-1"), eq(List.of()))).thenReturn(response);
    ActivityController controller = new ActivityController(service);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    MockMultipartFile activityPart =
        new MockMultipartFile(
            "activity",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            new ObjectMapper().findAndRegisterModules().writeValueAsBytes(validRequest()));

    mockMvc
        .perform(multipart("/activities").file(activityPart).principal(authentication("creator-1")))
        .andExpect(status().isCreated());
    verify(service).create(any(), eq("creator-1"), eq(List.of()));
  }

  private CreateActivityRequest validRequest() {
    return new CreateActivityRequest(
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
  }

  @Test
  void joinsParticipantAndReturnsOk() {
    IWeatherAdapter weatherAdapter = mock(IWeatherAdapter.class);
    ActivityController controller =
        new ActivityController(
            new ActivityService(
                new InMemoryActivityRepository(),
                weatherAdapter,
                new InMemoryUserRepository(),
                mock(StatisticsEventRecorder.class)));

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

    ActivityResponse activity =
        controller.create(request, List.of(), authentication("development-user")).getBody();

    ResponseEntity<ActivityResponse> response =
        controller.join(activity.id(), authentication("user-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(response.getBody()).isNotNull();

    assertThat(response.getBody().participantCount()).isEqualTo(1);

    assertThat(response.getBody().participants())
        .extracting(ParticipantDTO::userId)
        .containsExactly("user-1");
  }

  @Test
  void removesParticipantAndReturnsOk() {
    IWeatherAdapter weatherAdapter = mock(IWeatherAdapter.class);
    ActivityController controller =
        new ActivityController(
            new ActivityService(
                new InMemoryActivityRepository(),
                weatherAdapter,
                new InMemoryUserRepository(),
                mock(StatisticsEventRecorder.class)));

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

    ActivityResponse activity =
        controller.create(request, List.of(), authentication("development-user")).getBody();

    controller.join(activity.id(), authentication("user-1"));

    ResponseEntity<ActivityResponse> response =
        controller.leave(activity.id(), authentication("user-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(response.getBody()).isNotNull();

    assertThat(response.getBody().participantCount()).isZero();

    assertThat(response.getBody().participants()).isEmpty();
  }

  @Test
  void returnsWeatherAndForecastForParticipant() {
    IWeatherAdapter weatherAdapter = mock(IWeatherAdapter.class);

    ActivityController controller =
        new ActivityController(
            new ActivityService(
                new InMemoryActivityRepository(),
                weatherAdapter,
                new InMemoryUserRepository(),
                mock(StatisticsEventRecorder.class)));

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

    ActivityResponse activity =
        controller.create(request, List.of(), authentication("development-user")).getBody();

    controller.join(activity.id(), authentication("user-1"));

    WeatherForecast currentWeather =
        new WeatherForecast(LocalDateTime.of(2026, 8, 27, 10, 0), 22.0f, 10.0f, 15.0f);

    WeatherForecast activityForecast =
        new WeatherForecast(activity.dateTime(), 18.0f, 60.0f, 30.0f);

    when(weatherAdapter.getWeather(any())).thenReturn(currentWeather);

    when(weatherAdapter.getFutureClimate(any(), eq(activity.dateTime())))
        .thenReturn(activityForecast);

    ResponseEntity<ActivityWeatherResponse> response =
        controller.getWeather(activity.id(), authentication("user-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(response.getBody()).isNotNull();

    assertThat(response.getBody().activityId()).isEqualTo(activity.id());

    assertThat(response.getBody().location()).isEqualTo(activity.location());

    assertThat(response.getBody().activityDateTime()).isEqualTo(activity.dateTime());

    assertThat(response.getBody().currentWeather().temperature()).isEqualTo(22.0f);

    assertThat(response.getBody().activityForecast().temperature()).isEqualTo(18.0f);
  }

  @Test
  void listsActivitiesOrganizedByCurrentUser() throws Exception {
    ActivityService service = mock(ActivityService.class);
    when(service.getByOrganizerId("user-1")).thenReturn(List.of());
    ActivityController controller = new ActivityController(service);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc
        .perform(get("/activities/organizers/me").principal(authentication("user-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    verify(service).getByOrganizerId("user-1");
  }

  @Test
  void listsActivitiesJoinedByCurrentUser() throws Exception {
    ActivityService service = mock(ActivityService.class);
    when(service.getByParticipantId("user-1")).thenReturn(List.of());
    ActivityController controller = new ActivityController(service);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc
        .perform(get("/activities/participants/me").principal(authentication("user-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    verify(service).getByParticipantId("user-1");
  }

  private TestingAuthenticationToken authentication(String userId) {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId, null);
    authentication.setAuthenticated(true);
    return authentication;
  }
}
