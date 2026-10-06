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
import com.solnotfound.dto.PageResponse;
import com.solnotfound.dto.ParticipantDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.dto.WeatherForecastDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.City;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
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
import com.solnotfound.repository.InMemoryVotationRepository;
import com.solnotfound.storage.ImageFile;
import com.solnotfound.storage.ImageStorage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class ActivityServiceTest {

  private ActivityService activityService;
  private IActivityRepository activityRepository;
  private IWeatherAdapter weatherAdapter;
  private IUserRepository userRepository;
  private StatisticsEventRecorder statisticsRecorder;
  private InMemoryVotationRepository votationRepository;

  @BeforeEach
  void setUp() {
    activityRepository = new InMemoryActivityRepository();
    weatherAdapter = org.mockito.Mockito.mock(IWeatherAdapter.class);
    userRepository = new InMemoryUserRepository();
    statisticsRecorder = org.mockito.Mockito.mock(StatisticsEventRecorder.class);
    votationRepository = new InMemoryVotationRepository();

    activityService =
        new ActivityService(
            activityRepository,
            weatherAdapter,
            userRepository,
            statisticsRecorder,
            votationRepository);
  }

  @Test
  void createsActivityAndMapsWeatherConditions() {
    ActivityResponse created = activityService.create(validRequest());

    assertThat(created.id()).isNotBlank();
    assertThat(created.title()).isEqualTo("Football match");
    assertThat(created.organizerId()).isEqualTo("development-user");
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
        .hasMessage("Images must be JPEG, PNG, or WebP")
        .extracting("code")
        .isEqualTo(com.solnotfound.exception.ErrorCode.INVALID_IMAGE);
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
        activityRepository,
        weatherAdapter,
        userRepository,
        statisticsRecorder,
        votationRepository,
        imageStorage);
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
  void judgesFutureDateTimeAgainstTheCallersTimeZoneRatherThanTheServersClock() {
    // Same wall-clock reading, five minutes after "now" as seen from UTC-12 — which is
    // 26 hours behind UTC+14, so the identical literal is in the future in -12 but already
    // well in the past in +14. A server-clock-only check (the original bug) would judge both
    // requests identically regardless of which zone the organizer actually meant.
    LocalDateTime organizerLocalTime =
        LocalDateTime.ofInstant(Instant.now(), ZoneOffset.ofHours(-12)).plusMinutes(5);
    CreateActivityRequest request = requestAt(organizerLocalTime);

    ActivityResponse created = activityService.create(request, "creator-1", List.of(), "-12:00");
    assertThat(created.id()).isNotBlank();

    assertThatThrownBy(() -> activityService.create(request, "creator-1", List.of(), "+14:00"))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Activity date and time must be in the future");
  }

  @Test
  void storesTheOrganizerTimeZoneSoDeadlinesAreEvaluatedInIt() {
    CreateActivityRequest request = requestAt(LocalDateTime.now().plusDays(2));

    ActivityResponse created =
        activityService.create(request, "creator-1", List.of(), "America/Argentina/Buenos_Aires");

    assertThat(activityRepository.findById(created.id()).getTimeZone())
        .isEqualTo("America/Argentina/Buenos_Aires");
    // And it travels back to the client: without it the UI cannot tell whether an
    // already-past date is really past, since its own clock is a different zone.
    assertThat(created.timeZone()).isEqualTo("America/Argentina/Buenos_Aires");
  }

  @Test
  void rejectsUnknownTimeZoneId() {
    CreateActivityRequest request = requestAt(LocalDateTime.now().plusDays(1));

    assertThatThrownBy(() -> activityService.create(request, "creator-1", List.of(), "Not/AZone"))
        .isInstanceOf(InvalidActivityException.class)
        .hasMessage("Unknown time zone: Not/AZone");
  }

  private CreateActivityRequest requestAt(LocalDateTime dateTime) {
    return new CreateActivityRequest(
        "Football match",
        "Friendly match",
        ActivityType.OUTDOOR,
        cityLocation(),
        dateTime,
        10,
        20,
        validWeatherConditions(),
        4,
        validReprogramationRange());
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
  void searchFiltersByPartialCityCaseInsensitively() {
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Cordoba", LocalDateTime.now().plusDays(1)));

    List<ActivityResponse> results =
        activityService.search(new ActivityFilterDTO(null, " BUENOS ", null, null, null));

    assertThat(results)
        .extracting(response -> response.location().city())
        .containsExactly("Buenos Aires");
  }

  @Test
  void searchFiltersByTitleIgnoringCaseAndSurroundingSpaces() {
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    CreateActivityRequest barbecue =
        new CreateActivityRequest(
            "Asado en la plaza",
            "Traer sillas",
            ActivityType.OUTDOOR,
            new LocationDTO("Buenos Aires", null, null),
            LocalDateTime.now().plusDays(1),
            10,
            20,
            validWeatherConditions(),
            4,
            validReprogramationRange());
    activityService.create(barbecue);

    List<ActivityResponse> results =
        activityService.search(
            new ActivityFilterDTO(null, null, null, null, null, List.of(), "  ASADO ", List.of()));

    assertThat(results).extracting(ActivityResponse::title).containsExactly("Asado en la plaza");
  }

  @Test
  void searchFiltersByIds() {
    ActivityResponse first =
        activityService.create(
            requestWith(ActivityType.OUTDOOR, "Buenos Aires", LocalDateTime.now().plusDays(1)));
    activityService.create(
        requestWith(ActivityType.OUTDOOR, "Cordoba", LocalDateTime.now().plusDays(1)));

    List<ActivityResponse> results =
        activityService.search(
            new ActivityFilterDTO(
                null, null, null, null, null, List.of(), null, List.of(first.id(), "missing")));

    assertThat(results).extracting(ActivityResponse::id).containsExactly(first.id());
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
  void reportsTheActivityAsFullWhenAnotherUserTakesTheLastSpotConcurrently() {
    // Both requests read the activity while it still had a spot; the other one wins the atomic
    // update, so this one must reload, see it full and fail instead of overbooking.
    IActivityRepository repository = mock(IActivityRepository.class);
    Activity withOneSpot = activityWithCapacity(1);
    Activity full = activityWithCapacity(1);
    full.addParticipant("rival");
    when(repository.findById("activity-1")).thenReturn(withOneSpot, full);
    when(repository.addParticipant("activity-1", "user-1")).thenReturn(false);
    ActivityService service =
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

    assertThatThrownBy(() -> service.join("activity-1", "user-1"))
        .isInstanceOf(IllegalStateActivityException.class)
        .extracting("code")
        .isEqualTo(com.solnotfound.exception.ErrorCode.ACTIVITY_FULL);
    verify(repository, never()).save(any());
  }

  @Test
  void joinsWithAnAtomicUpdateInsteadOfSavingTheWholeActivity() {
    IActivityRepository repository = mock(IActivityRepository.class);
    when(repository.findById("activity-1")).thenReturn(activityWithCapacity(5));
    when(repository.addParticipant("activity-1", "user-1")).thenReturn(true);
    ActivityService service =
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

    ActivityResponse joined = service.join("activity-1", "user-1");

    assertThat(joined.participants()).extracting(ParticipantDTO::userId).containsExactly("user-1");
    verify(repository).addParticipant("activity-1", "user-1");
    verify(repository, never()).save(any());
  }

  @Test
  void joiningAgainDoesNotTouchTheStoredParticipants() {
    IActivityRepository repository = mock(IActivityRepository.class);
    Activity activity = activityWithCapacity(5);
    activity.addParticipant("user-1");
    when(repository.findById("activity-1")).thenReturn(activity);
    ActivityService service =
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

    service.join("activity-1", "user-1");

    verify(repository, never()).addParticipant(any(), any());
    verify(repository, never()).save(any());
  }

  @Test
  void leavesWithAnAtomicUpdateInsteadOfSavingTheWholeActivity() {
    IActivityRepository repository = mock(IActivityRepository.class);
    Activity activity = activityWithCapacity(5);
    activity.addParticipant("user-1");
    when(repository.findById("activity-1")).thenReturn(activity);
    ActivityService service =
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

    ActivityResponse left = service.leave("activity-1", "user-1");

    assertThat(left.participants()).isEmpty();
    verify(repository).removeParticipant("activity-1", "user-1");
    verify(repository, never()).save(any());
  }

  @Test
  void leavingRemovesTheVoteOfTheParticipantFromTheActiveVotation() {
    Activity activity = activityWithCapacity(5);
    activity.addParticipant("user-1");
    activity.addParticipant("user-2");
    activityRepository.save(activity);
    Votation votation = votationVotedBy(activity, "user-1", "user-2");

    activityService.leave("activity-1", "user-1");

    assertThat(votation.getVoteByUserId("user-1")).isEmpty();
    assertThat(votation.getVoteByUserId("user-2")).isPresent();
  }

  @Test
  void organizerKeepsTheirVoteWhenTheyStopBeingAParticipant() {
    Activity activity = activityWithCapacity(5);
    activity.addParticipant("organizer");
    activityRepository.save(activity);
    Votation votation = votationVotedBy(activity, "organizer");

    activityService.leave("activity-1", "organizer");

    assertThat(votation.getVoteByUserId("organizer")).isPresent();
  }

  @Test
  void leavingDoesNotTouchVotesOfClosedVotations() {
    Activity activity = activityWithCapacity(5);
    activity.addParticipant("user-1");
    activityRepository.save(activity);
    Votation votation = votationVotedBy(activity, "user-1");
    votation.setStatus(VotationStatus.CLOSED);

    activityService.leave("activity-1", "user-1");

    assertThat(votation.getVoteByUserId("user-1")).isPresent();
  }

  private Votation votationVotedBy(Activity activity, String... voterIds) {
    VotationOption option = new VotationOption();
    option.setDateTime(activity.getDateTime().plusDays(1));
    option.setUsers(java.util.Arrays.stream(voterIds).map(User::withId).toList());
    Votation votation = new Votation();
    votation.setId("votation-1");
    votation.setActivity(activity);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setOptions(List.of(option));
    votationRepository.save(votation);
    return votation;
  }

  private Activity activityWithCapacity(int maxParticipants) {
    Activity activity = new Activity();
    activity.setId("activity-1");
    activity.setTitle("Football match");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Buenos Aires"), null, null));
    activity.setDateTime(LocalDateTime.now().plusDays(1));
    activity.setMinParticipants(1);
    activity.setMaxParticipants(maxParticipants);
    activity.setOrganizer(User.withId("organizer"));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(21, 0)));
    return activity;
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
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

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
        new ActivityService(
            repository, weatherAdapter, userRepository, statisticsRecorder, votationRepository);

    assertThatThrownBy(() -> service.getByParticipantId("1"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void pagedOrganizerQueryFiltersByDateRange() {
    LocalDateTime inRange = LocalDateTime.now().plusDays(10);
    LocalDateTime outOfRange = LocalDateTime.now().plusDays(30);
    ActivityResponse inside =
        activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", inRange));
    ActivityResponse outside =
        activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", outOfRange));
    activityRepository.findById(inside.id()).setOrganizer(user("1"));
    activityRepository.findById(outside.id()).setOrganizer(user("1"));

    PageResponse<ActivityResponse> result =
        activityService.getByOrganizerId(
            "1", LocalDateTime.now(), LocalDateTime.now().plusDays(15), PageRequest.of(0, 12));

    assertThat(result.content()).extracting(ActivityResponse::id).containsExactly(inside.id());
  }

  @Test
  void pagedOrganizerQueryWithoutDateRangeReturnsEverything() {
    ActivityResponse first = activityService.create(validRequest());
    ActivityResponse second = activityService.create(validRequest());
    activityRepository.findById(first.id()).setOrganizer(user("1"));
    activityRepository.findById(second.id()).setOrganizer(user("1"));

    PageResponse<ActivityResponse> result =
        activityService.getByOrganizerId("1", null, null, PageRequest.of(0, 12));

    assertThat(result.content())
        .extracting(ActivityResponse::id)
        .containsExactlyInAnyOrder(first.id(), second.id());
  }

  @Test
  void pagedParticipantQueryFiltersByDateRange() {
    LocalDateTime inRange = LocalDateTime.now().plusDays(10);
    LocalDateTime outOfRange = LocalDateTime.now().plusDays(30);
    ActivityResponse inside =
        activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", inRange));
    ActivityResponse outside =
        activityService.create(requestWith(ActivityType.OUTDOOR, "Buenos Aires", outOfRange));
    activityRepository.findById(inside.id()).setParticipants(List.of(user("1")));
    activityRepository.findById(outside.id()).setParticipants(List.of(user("1")));

    PageResponse<ActivityResponse> result =
        activityService.getByParticipantId(
            "1", LocalDateTime.now(), LocalDateTime.now().plusDays(15), PageRequest.of(0, 12));

    assertThat(result.content()).extracting(ActivityResponse::id).containsExactly(inside.id());
  }

  @Test
  void pagedParticipantQueryWithoutDateRangeReturnsEverything() {
    ActivityResponse first = activityService.create(validRequest());
    ActivityResponse second = activityService.create(validRequest());
    activityRepository.findById(first.id()).setParticipants(List.of(user("1")));
    activityRepository.findById(second.id()).setParticipants(List.of(user("1")));

    PageResponse<ActivityResponse> result =
        activityService.getByParticipantId("1", null, null, PageRequest.of(0, 12));

    assertThat(result.content())
        .extracting(ActivityResponse::id)
        .containsExactlyInAnyOrder(first.id(), second.id());
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
