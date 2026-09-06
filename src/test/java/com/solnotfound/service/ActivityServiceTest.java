package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.ActivityWeatherResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ParticipantDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.dto.WeatherForecastDTO;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.ActivityAccessDeniedException;
import com.solnotfound.exception.ActivityNotFoundException;
import com.solnotfound.exception.IllegalStateActivityException;
import com.solnotfound.exception.ImageStorageException;
import com.solnotfound.exception.InvalidActivityException;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.InMemoryActivityRepository;
import com.solnotfound.repository.InMemoryUserRepository;
import com.solnotfound.storage.ImageFile;
import com.solnotfound.storage.ImageStorage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {

  private ActivityService activityService;
  private IActivityRepository activityRepository;
  private IWeatherAdapter weatherAdapter;
  private IUserRepository userRepository;
  private StatisticsEventRecorder statisticsRecorder;

  @BeforeEach
  void setUp() {
    activityRepository = new InMemoryActivityRepository();
    weatherAdapter = org.mockito.Mockito.mock(IWeatherAdapter.class);
    userRepository = new InMemoryUserRepository();
    statisticsRecorder = org.mockito.Mockito.mock(StatisticsEventRecorder.class);

    activityService =
        new ActivityService(activityRepository, weatherAdapter, userRepository, statisticsRecorder);
  }

  @Test
  void createsActivityAndMapsWeatherConditions() {
    ActivityResponse created = activityService.create(validRequest());

    assertThat(created.id()).isNotBlank();
    assertThat(created.title()).isEqualTo("Football match");
    assertThat(created.weatherConditions()).isEqualTo(new WeatherConditionsDTO(30, 10, 28, 25.0));
    assertThat(created.reprogramationRange()).isEqualTo(validReprogramationRange());
    assertThat(activityService.getById(created.id())).isEqualTo(created);
  }

  @Test
  void createsActivityWithImagesAndReturnsSignedUrls() {
    ImageStorage imageStorage = mock(ImageStorage.class);
    when(imageStorage.signedReadUrl(any(), any(Duration.class)))
        .thenAnswer(invocation -> URI.create("https://images.test/" + invocation.getArgument(0)));
    activityService = serviceWith(imageStorage);

    ActivityResponse created =
        activityService.create(
            validRequest(), "creator-1", List.of(image("image/jpeg"), image("image/webp")));

    assertThat(created.imageUrls())
        .hasSize(2)
        .allMatch(url -> url.startsWith("https://images.test/activities/" + created.id() + "/"));
    verify(imageStorage).upload(org.mockito.ArgumentMatchers.endsWith(".jpg"), any());
    verify(imageStorage).upload(org.mockito.ArgumentMatchers.endsWith(".webp"), any());
  }

  @Test
  void rejectsUnsupportedImageTypeBeforeUploading() {
    ImageStorage imageStorage = mock(ImageStorage.class);
    activityService = serviceWith(imageStorage);

    assertThatThrownBy(
            () -> activityService.create(validRequest(), "creator-1", List.of(image("image/gif"))))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Images must be JPEG, PNG, or WebP");
    verify(imageStorage, never()).upload(any(), any());
  }

  @Test
  void deletesUploadedImagesAndDoesNotPersistWhenAnUploadFails() {
    ImageStorage imageStorage = mock(ImageStorage.class);
    doThrow(new ImageStorageException("failed", null))
        .when(imageStorage)
        .upload(org.mockito.ArgumentMatchers.endsWith(".png"), any());
    activityService = serviceWith(imageStorage);

    assertThatThrownBy(
            () ->
                activityService.create(
                    validRequest(), "creator-1", List.of(image("image/jpeg"), image("image/png"))))
        .isInstanceOf(ImageStorageException.class);
    verify(imageStorage).delete(org.mockito.ArgumentMatchers.endsWith(".jpg"));
    assertThat(activityRepository.findAll()).isEmpty();
  }

  private ActivityService serviceWith(ImageStorage imageStorage) {
    return new ActivityService(
        activityRepository, weatherAdapter, userRepository, statisticsRecorder, imageStorage);
  }

  private ImageFile image(String contentType) {
    return new ImageFile() {
      @Override
      public String contentType() {
        return contentType;
      }

      @Override
      public long size() {
        return 3;
      }

      @Override
      public ByteArrayInputStream openStream() {
        return new ByteArrayInputStream(new byte[] {1, 2, 3});
      }
    };
  }

  @Test
  void rejectsReprogramationRangeWithInitialHourAfterFinalHour() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            validWeatherConditions(),
            4,
            new ReprogramationRangeDTO(3, LocalTime.of(20, 0), LocalTime.of(10, 0)));

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Reprogramation range initial hour must not be after final hour");
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
            15,
            validReprogramationRange());

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
            3,
            validReprogramationRange());

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
            4,
            validReprogramationRange());

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
            3,
            validReprogramationRange());

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
            2,
            validReprogramationRange());

    assertThatThrownBy(() -> activityService.create(request))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Latitude and longitude must be provided together");
  }

  @Test
  void searchWithNoFiltersReturnsEveryActivity() {
    activityService.create(validRequest());
    activityService.create(
        requestWith(ActivityType.INDOOR, "Cordoba", LocalDateTime.now().plusDays(5)));

    assertThat(activityService.search(new ActivityFilterDTO(null, null, null, null, null)))
        .hasSize(2);
  }

  @Test
  void searchFiltersByType() {
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    activityService.create(
        requestWith(ActivityType.INDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));

    List<ActivityResponse> results =
        activityService.search(new ActivityFilterDTO(ActivityType.INDOOR, null, null, null, null));

    assertThat(results).extracting(ActivityResponse::type).containsExactly(ActivityType.INDOOR);
  }

  @Test
  void searchFiltersByCityCaseInsensitively() {
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Cordoba", LocalDateTime.now().plusDays(1)));

    List<ActivityResponse> results =
        activityService.search(new ActivityFilterDTO(null, "buenos aires", null, null, null));

    assertThat(results)
        .extracting(response -> response.location().city())
        .containsExactly("Buenos Aires");
  }

  @Test
  void searchFiltersByDateRange() {
    LocalDateTime soon = LocalDateTime.now().plusDays(1);
    LocalDateTime farAway = LocalDateTime.now().plusDays(10);
    activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", soon));
    activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", farAway));

    List<ActivityResponse> results =
        activityService.search(
            new ActivityFilterDTO(
                null, null, LocalDateTime.now(), LocalDateTime.now().plusDays(3), null));

    assertThat(results).extracting(ActivityResponse::dateTime).containsExactly(soon);
  }

  @Test
  void searchRejectsInvertedDateRange() {
    LocalDateTime dateFrom = LocalDateTime.of(2026, 9, 10, 0, 0);
    LocalDateTime dateTo = LocalDateTime.of(2026, 9, 1, 0, 0);

    assertThatThrownBy(
            () -> activityService.search(new ActivityFilterDTO(null, null, dateFrom, dateTo, null)))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Search start date cannot be after end date");
  }

  @Test
  void searchFiltersByAvailability() {
    ActivityResponse available =
        activityService.create(
            requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));

    ActivityResponse full =
        activityService.create(
            requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    for (int i = 0; i < full.maxParticipants(); i++) {
      activityService.join(full.id(), "user-" + i);
    }

    List<ActivityResponse> results =
        activityService.search(new ActivityFilterDTO(null, null, null, null, true));

    assertThat(results).extracting(ActivityResponse::id).containsExactly(available.id());
  }

  @Test
  void joinsParticipantToActivity() {
    ActivityResponse activity = activityService.create(validRequest());

    ActivityResponse result = activityService.join(activity.id(), "user-1");

    assertThat(result.participantCount()).isEqualTo(1);

    assertThat(result.participants()).extracting(ParticipantDTO::userId).containsExactly("user-1");
  }

  @Test
  void removesParticipantFromActivity() {
    ActivityResponse activity = activityService.create(validRequest());

    activityService.join(activity.id(), "user-1");

    ActivityResponse result = activityService.leave(activity.id(), "user-1");

    assertThat(result.participantCount()).isZero();

    assertThat(result.participants()).isEmpty();
  }

  @Test
  void rejectsJoiningNonExistentActivity() {
    assertThatThrownBy(() -> activityService.join("non-existent-id", "user-1"))
        .isInstanceOf(ActivityNotFoundException.class)
        .hasMessage("Activity not found: non-existent-id");
  }

  @Test
  void rejectsLeavingNonExistentActivity() {
    assertThatThrownBy(() -> activityService.leave("non-existent-id", "user-1"))
        .isInstanceOf(ActivityNotFoundException.class)
        .hasMessage("Activity not found: non-existent-id");
  }

  @Test
  void rejectsJoiningActivityTwice() {
    ActivityResponse activity = activityService.create(validRequest());

    activityService.join(activity.id(), "user-1");

    ActivityResponse result = activityService.join(activity.id(), "user-1");

    assertThat(result.participants()).extracting(ParticipantDTO::userId).containsExactly("user-1");
  }

  @Test
  void rejectsJoiningWhenActivityIsFull() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            1,
            1,
            validWeatherConditions(),
            4,
            validReprogramationRange());

    ActivityResponse activity = activityService.create(request);

    activityService.join(activity.id(), "user-1");

    assertThatThrownBy(() -> activityService.join(activity.id(), "user-2"))
        .isInstanceOf(IllegalStateActivityException.class)
        .hasMessage("Activity has no available spots.");
  }

  @Test
  void activityRemainsAvailableWhileItHasCapacity() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            2,
            3,
            validWeatherConditions(),
            4,
            validReprogramationRange());

    ActivityResponse activity = activityService.create(request);

    ActivityResponse afterFirstJoin = activityService.join(activity.id(), "user-1");

    assertThat(afterFirstJoin.availability()).isTrue();

    ActivityResponse afterSecondJoin = activityService.join(activity.id(), "user-2");

    assertThat(afterSecondJoin.availability()).isTrue();
  }

  @Test
  void activityBecomesAvailableAgainWhenAFullActivityLosesAParticipant() {
    CreateActivityRequest request =
        new CreateActivityRequest(
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            cityLocation(),
            LocalDateTime.now().plusDays(1),
            2,
            3,
            validWeatherConditions(),
            4,
            validReprogramationRange());

    ActivityResponse activity = activityService.create(request);

    activityService.join(activity.id(), "user-1");
    activityService.join(activity.id(), "user-2");
    activityService.join(activity.id(), "user-3");

    assertThat(activityService.getById(activity.id()).availability()).isFalse();

    ActivityResponse result = activityService.leave(activity.id(), "user-1");

    assertThat(result.availability()).isTrue();
  }

  @Test
  void getsCurrentWeatherAndActivityForecastForParticipant() {
    ActivityResponse activity = activityService.create(validRequest());

    activityService.join(activity.id(), "user-1");

    LocalDateTime currentDateTime = LocalDateTime.now();

    WeatherForecast currentWeather = new WeatherForecast(currentDateTime, 25.0f, 20.0f, 10.0f);

    WeatherForecast activityForecast =
        new WeatherForecast(activity.dateTime(), 22.0f, 40.0f, 15.0f);

    when(weatherAdapter.getWeather(any(Location.class))).thenReturn(currentWeather);

    when(weatherAdapter.getFutureClimate(
            any(Location.class), org.mockito.ArgumentMatchers.eq(activity.dateTime())))
        .thenReturn(activityForecast);

    ActivityWeatherResponse result = activityService.getWeather(activity.id(), "user-1");

    assertThat(result.activityId()).isEqualTo(activity.id());
    assertThat(result.location()).isEqualTo(activity.location());
    assertThat(result.activityDateTime()).isEqualTo(activity.dateTime());

    assertThat(result.currentWeather())
        .isEqualTo(new WeatherForecastDTO(currentDateTime, 25.0f, 20.0f, 10.0f));

    assertThat(result.activityForecast())
        .isEqualTo(new WeatherForecastDTO(activity.dateTime(), 22.0f, 40.0f, 15.0f));
  }

  @Test
  void rejectsGettingWeatherForNonExistentActivity() {
    assertThatThrownBy(() -> activityService.getWeather("non-existent-id", "user-1"))
        .isInstanceOf(ActivityNotFoundException.class)
        .hasMessage("Activity not found: non-existent-id");

    verify(weatherAdapter, never()).getWeather(any(Location.class));
    verify(weatherAdapter, never()).getFutureClimate(any(Location.class), any(LocalDateTime.class));
  }

  @Test
  void rejectsGettingWeatherForUserWhoIsNotParticipant() {
    ActivityResponse activity = activityService.create(validRequest());

    assertThatThrownBy(() -> activityService.getWeather(activity.id(), "user-1"))
        .isInstanceOf(ActivityAccessDeniedException.class)
        .hasMessage("User is not participating in this activity");

    verify(weatherAdapter, never()).getWeather(any(Location.class));

    verify(weatherAdapter, never()).getFutureClimate(any(Location.class), any(LocalDateTime.class));
  }

  @Test
  void returnsEmptyListWhenNoActivitiesExist() {
    assertThat(activityService.getByOrganizerId("1")).isEmpty();
    assertThat(activityService.getByParticipantId("1")).isEmpty();
  }

  @Test
  void returnsActivitiesOrganizedByTheUser() {
    ActivityResponse first = activityService.create(validRequest());
    ActivityResponse second = activityService.create(validRequest());
    ActivityResponse other = activityService.create(validRequest());
    activityRepository.findById(first.id()).setOrganizer(user("1"));
    activityRepository.findById(second.id()).setOrganizer(user("1"));
    activityRepository.findById(other.id()).setOrganizer(user("2"));

    List<ActivityResponse> results = activityService.getByOrganizerId("1");

    assertThat(results)
        .extracting(ActivityResponse::id)
        .containsExactlyInAnyOrder(first.id(), second.id());
  }

  @Test
  void organizerQueryExcludesActivitiesWhereTheUserOnlyParticipates() {
    ActivityResponse organized = activityService.create(validRequest());
    ActivityResponse participated = activityService.create(validRequest());
    activityRepository.findById(organized.id()).setOrganizer(user("1"));
    activityRepository.findById(participated.id()).setParticipants(List.of(user("1")));

    List<ActivityResponse> results = activityService.getByOrganizerId("1");

    assertThat(results).extracting(ActivityResponse::id).containsExactly(organized.id());
  }

  @Test
  void organizerQueryReturnsEmptyListWhenUserOrganizesNoActivity() {
    ActivityResponse other = activityService.create(validRequest());
    activityRepository.findById(other.id()).setOrganizer(user("2"));

    assertThat(activityService.getByOrganizerId("1")).isEmpty();
  }

  @Test
  void organizerQueryIgnoresActivitiesWithoutOrganizer() {
    activityService.create(validRequest());
    ActivityResponse organized = activityService.create(validRequest());
    activityRepository.findById(organized.id()).setOrganizer(user("1"));

    List<ActivityResponse> results = activityService.getByOrganizerId("1");

    assertThat(results).extracting(ActivityResponse::id).containsExactly(organized.id());
  }

  @Test
  void organizerQueryRejectsInvalidRepositoryResult() {
    IActivityRepository repository = mock(IActivityRepository.class);
    when(repository.findActivitiesByOrganizerId("1")).thenReturn(null);
    ActivityService service =
        new ActivityService(repository, weatherAdapter, userRepository, statisticsRecorder);

    assertThatThrownBy(() -> service.getByOrganizerId("1"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void returnsActivitiesWhereTheUserIsParticipant() {
    ActivityResponse first = activityService.create(validRequest());
    ActivityResponse second = activityService.create(validRequest());
    ActivityResponse other = activityService.create(validRequest());
    activityRepository.findById(first.id()).setParticipants(List.of(user("1")));
    activityRepository.findById(second.id()).setParticipants(List.of(user("1"), user("3")));
    activityRepository.findById(other.id()).setParticipants(List.of(user("2")));

    List<ActivityResponse> results = activityService.getByParticipantId("1");

    assertThat(results)
        .extracting(ActivityResponse::id)
        .containsExactlyInAnyOrder(first.id(), second.id());
  }

  @Test
  void participantQueryExcludesActivitiesTheUserOnlyOrganizes() {
    ActivityResponse organized = activityService.create(validRequest());
    ActivityResponse participated = activityService.create(validRequest());
    activityRepository.findById(organized.id()).setOrganizer(user("1"));
    activityRepository.findById(participated.id()).setParticipants(List.of(user("1")));

    List<ActivityResponse> results = activityService.getByParticipantId("1");

    assertThat(results).extracting(ActivityResponse::id).containsExactly(participated.id());
  }

  @Test
  void participantQueryReturnsEmptyListWhenUserParticipatesInNoActivity() {
    ActivityResponse other = activityService.create(validRequest());
    activityRepository.findById(other.id()).setParticipants(List.of(user("2")));

    assertThat(activityService.getByParticipantId("1")).isEmpty();
  }

  @Test
  void participantQueryIgnoresActivitiesWithoutParticipants() {
    activityService.create(validRequest());
    ActivityResponse participated = activityService.create(validRequest());
    activityRepository.findById(participated.id()).setParticipants(List.of(user("1")));

    List<ActivityResponse> results = activityService.getByParticipantId("1");

    assertThat(results).extracting(ActivityResponse::id).containsExactly(participated.id());
  }

  @Test
  void participantQueryRejectsInvalidRepositoryResult() {
    IActivityRepository repository = mock(IActivityRepository.class);
    when(repository.findActivitiesByParticipantId("1")).thenReturn(null);
    ActivityService service =
        new ActivityService(repository, weatherAdapter, userRepository, statisticsRecorder);

    assertThatThrownBy(() -> service.getByParticipantId("1"))
        .isInstanceOf(NullPointerException.class);
  }

  private CreateActivityRequest requestWith(
      ActivityType type, String city, LocalDateTime dateTime) {
    return new CreateActivityRequest(
        "Football match",
        "Friendly match",
        type,
        new LocationDTO(city, null, null),
        dateTime,
        10,
        20,
        validWeatherConditions(),
        4,
        validReprogramationRange());
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
        4,
        validReprogramationRange());
  }

  private LocationDTO cityLocation() {
    return new LocationDTO("Buenos Aires", null, null);
  }

  private WeatherConditionsDTO validWeatherConditions() {
    return new WeatherConditionsDTO(30, 10, 28, 25.0);
  }

  private ReprogramationRangeDTO validReprogramationRange() {
    return new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0));
  }

  private User user(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
