package com.solnotfound.service;

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
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.entity.weather.MaxRainProbabilityCondition;
import com.solnotfound.entity.weather.MaxWindCondition;
import com.solnotfound.entity.weather.TemperatureRangeCondition;
import com.solnotfound.entity.weather.WeatherCondition;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.ActivityAccessDeniedException;
import com.solnotfound.exception.ActivityNotFoundException;
import com.solnotfound.exception.InvalidActivityException;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.ICityRepository;
import com.solnotfound.repository.InMemoryStatisticsEventRepository;
import com.solnotfound.storage.ImageFile;
import com.solnotfound.storage.ImageStorage;
import com.solnotfound.storage.NoOpImageStorage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
  private final IActivityRepository activityRepository;
  private final IWeatherAdapter weatherAdapter;
  private final ICityRepository cityRepository;
  private final StatisticsEventRecorder statisticsRecorder;
  private final ImageStorage imageStorage;
  private static final int MAX_IMAGES = 5;
  private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
  private static final Duration IMAGE_URL_VALIDITY = Duration.ofHours(1);
  private static final Set<String> ALLOWED_IMAGE_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared in-memory repository")
  @Autowired
  public ActivityService(
      IActivityRepository activityRepository,
      IWeatherAdapter weatherAdapter,
      ICityRepository cityRepository,
      StatisticsEventRecorder statisticsRecorder,
      ImageStorage imageStorage) {
    this.activityRepository = activityRepository;
    this.weatherAdapter = weatherAdapter;
    this.cityRepository = cityRepository;
    this.statisticsRecorder = statisticsRecorder;
    this.imageStorage = imageStorage;
  }

  public ActivityService(
      IActivityRepository activityRepository,
      IWeatherAdapter weatherAdapter,
      ICityRepository cityRepository) {
    this(
        activityRepository,
        weatherAdapter,
        cityRepository,
        new StatisticsEventRecorder(new InMemoryStatisticsEventRepository()),
        new NoOpImageStorage());
  }

  public ActivityResponse create(CreateActivityRequest request) {
    return create(request, "development-user");
  }

  /**
   * Validates and creates an activity associated with its authenticated creator.
   *
   * @param request activity data to validate and persist
   * @param creatorUserId authenticated creator identifier
   * @return the persisted activity representation
   * @throws InvalidActivityException when cross-field business constraints are not satisfied
   */
  public ActivityResponse create(CreateActivityRequest request, String creatorUserId) {
    return create(request, creatorUserId, List.of());
  }

  /**
   * Validates and creates an activity, uploading up to five optional JPEG, PNG, or WebP images.
   * Uploaded objects are removed if a later upload fails, and the activity is not persisted.
   *
   * @param request activity data to validate and persist
   * @param creatorUserId authenticated creator identifier
   * @param images optional image content, each limited to five MiB
   * @return the persisted activity representation with temporary image URLs
   * @throws InvalidActivityException when activity or image constraints are not satisfied
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
      justification = "Storage failures are propagated after compensating object deletes")
  public ActivityResponse create(
      CreateActivityRequest request, String creatorUserId, List<? extends ImageFile> images) {
    validate(request);
    validateImages(images);

    Activity activity = new Activity();
    activity.setId(UUID.randomUUID().toString());
    activity.setOrganizer(com.solnotfound.entity.user.User.withId(creatorUserId));
    activity.setTitle(request.title());
    activity.setDescription(request.description());
    activity.setType(request.type());
    activity.setLocation(toLocation(request.location()));
    activity.setDateTime(request.dateTime());
    activity.setMinParticipants(request.minParticipants());
    activity.setMaxParticipants(request.maxParticipants());
    activity.setWeatherConditions(toWeatherConditions(request.weatherConditions()));
    activity.setAnticipationWindow(request.anticipationWindow());
    activity.setReprogramationRange(toReprogramationRange(request.reprogramationRange()));

    List<String> uploadedKeys = new ArrayList<>();
    try {
      for (ImageFile image : images) {
        String imageKey = createImageKey(activity.getId(), image.contentType());
        imageStorage.upload(imageKey, image);
        uploadedKeys.add(imageKey);
      }
    } catch (RuntimeException exception) {
      uploadedKeys.forEach(this::deleteAfterFailedUpload);
      throw exception;
    }
    activity.setImageKeys(uploadedKeys);

    activityRepository.save(activity);
    statisticsRecorder.recordActivity(StatisticsEventType.ACTIVITY_CREATED, activity.getId(), null);

    return toResponse(activity);
  }

  private void validateImages(List<? extends ImageFile> images) {
    if (images.size() > MAX_IMAGES) {
      throw new InvalidActivityException("An activity can have at most 5 images");
    }
    for (ImageFile image : images) {
      String contentType = image.contentType();
      if (image.size() <= 0) {
        throw new InvalidActivityException("Images cannot be empty");
      }
      if (image.size() > MAX_IMAGE_SIZE) {
        throw new InvalidActivityException("Each image must be at most 5 MiB");
      }
      if (contentType == null
          || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
        throw new InvalidActivityException("Images must be JPEG, PNG, or WebP");
      }
    }
  }

  private String createImageKey(String activityId, String contentType) {
    String extension =
        switch (contentType.toLowerCase(Locale.ROOT)) {
          case "image/jpeg" -> ".jpg";
          case "image/png" -> ".png";
          case "image/webp" -> ".webp";
          default -> throw new InvalidActivityException("Unsupported image type");
        };
    return "activities/" + activityId + "/" + UUID.randomUUID() + extension;
  }

  private void deleteAfterFailedUpload(String imageKey) {
    try {
      imageStorage.delete(imageKey);
    } catch (RuntimeException ignored) {
      // Preserve the upload failure; orphan cleanup can be retried operationally.
    }
  }

  public List<ActivityResponse> getAll() {
    return activityRepository.findAll().stream().map(this::toResponse).toList();
  }

  /**
   * Searches activities using all non-null filters. Date bounds are inclusive.
   *
   * @param filter optional activity criteria
   * @return matching activities
   * @throws InvalidActivityException when the start date is after the end date
   */
  public List<ActivityResponse> search(ActivityFilterDTO filter) {
    if (filter.dateFrom() != null
        && filter.dateTo() != null
        && filter.dateFrom().isAfter(filter.dateTo())) {
      throw new InvalidActivityException("Search start date cannot be after end date");
    }

    return activityRepository.findAll().stream()
        .filter(activity -> matches(activity, filter))
        .map(this::toResponse)
        .toList();
  }

  private boolean matches(Activity activity, ActivityFilterDTO filter) {
    if (filter.type() != null && filter.type() != activity.getType()) {
      return false;
    }

    if (filter.city() != null
        && !filter.city().isBlank()
        && (activity.getLocation().city() == null
            || !filter.city().equalsIgnoreCase(activity.getLocation().city().name()))) {
      return false;
    }

    if (filter.availability() != null
        && !filter.availability().equals(activity.getAvailability())) {
      return false;
    }

    if (filter.dateFrom() != null && activity.getDateTime().isBefore(filter.dateFrom())) {
      return false;
    }

    return filter.dateTo() == null || !activity.getDateTime().isAfter(filter.dateTo());
  }

  public ActivityResponse getById(String id) {
    Activity activity = activityRepository.findById(id);
    if (activity == null) {
      return null;
    }
    return toResponse(activity);
  }

  /**
   * Adds a user to an activity and persists the resulting participant state.
   *
   * @param activityId activity identifier
   * @param userId authenticated user identifier
   * @return the updated activity
   * @throws ActivityNotFoundException when the activity does not exist
   */
  public ActivityResponse join(String activityId, String userId) {
    Activity activity = findActivityOrThrow(activityId);

    activity.addParticipant(userId);

    activityRepository.save(activity);

    return toResponse(activity);
  }

  /**
   * Removes a user from an activity and persists the resulting participant state.
   *
   * @param activityId activity identifier
   * @param userId authenticated user identifier
   * @return the updated activity
   * @throws ActivityNotFoundException when the activity does not exist
   */
  public ActivityResponse leave(String activityId, String userId) {
    Activity activity = findActivityOrThrow(activityId);

    activity.removeParticipant(userId);

    activityRepository.save(activity);

    return toResponse(activity);
  }

  /**
   * Retrieves current weather and the activity-time forecast for a participant.
   *
   * @param activityId activity identifier
   * @param userId authenticated user identifier
   * @return weather information associated with the activity
   * @throws ActivityNotFoundException when the activity does not exist
   * @throws ActivityAccessDeniedException when the user is not participating
   */
  public ActivityWeatherResponse getWeather(String activityId, String userId) {
    Activity activity = findActivityOrThrow(activityId);

    verifyParticipant(activity, userId);
    WeatherForecast currentWeather = weatherAdapter.getWeather(activity.getLocation());

    WeatherForecast activityForecast =
        weatherAdapter.getFutureClimate(activity.getLocation(), activity.getDateTime());

    return toWeatherResponse(activity, currentWeather, activityForecast);
  }

  public List<ActivityResponse> getByOrganizerId(String id) {
    List<Activity> activities = activityRepository.findActivitiesByOrganizerId(id);
    return activities.stream().map(this::toResponse).toList();
  }

  public List<ActivityResponse> getByParticipantId(String id) {
    List<Activity> activities = activityRepository.findActivitiesByParticipantId(id);
    return activities.stream().map(this::toResponse).toList();
  }

  private void validate(CreateActivityRequest request) {
    if (request.minParticipants() > request.maxParticipants()) {
      throw new InvalidActivityException("Minimum participants cannot exceed maximum participants");
    }

    validateLocation(request.location());
    validateWeatherConditions(request.weatherConditions());
    validateReprogramationRange(request.reprogramationRange());
  }

  private void validateLocation(LocationDTO location) {
    boolean hasCity = location.city() != null && !location.city().isBlank();
    boolean hasLatitude = location.latitude() != null;
    boolean hasLongitude = location.longitude() != null;
    boolean hasCoordinates = hasLatitude && hasLongitude;

    if (hasLatitude != hasLongitude) {
      throw new InvalidActivityException("Latitude and longitude must be provided together");
    }

    if (!hasCity && !hasCoordinates) {
      throw new InvalidActivityException("Location must contain a city or coordinates");
    }
  }

  private void validateWeatherConditions(WeatherConditionsDTO conditions) {
    boolean hasMinTemperature = conditions.minTemperature() != null;
    boolean hasMaxTemperature = conditions.maxTemperature() != null;

    if (hasMinTemperature != hasMaxTemperature) {
      throw new InvalidActivityException(
          "Minimum and maximum temperature must be provided together");
    }

    if (hasMinTemperature && conditions.minTemperature() > conditions.maxTemperature()) {
      throw new InvalidActivityException("Minimum temperature cannot exceed maximum temperature");
    }
  }

  private void validateReprogramationRange(ReprogramationRangeDTO range) {
    if (range.initialHour().isAfter(range.finalHour())) {
      throw new InvalidActivityException(
          "Reprogramation range initial hour must not be after final hour");
    }
  }

  private void verifyParticipant(Activity activity, String userId) {
    boolean isParticipant =
        activity.getParticipants().stream().anyMatch(p -> p.getId().equals(userId));

    if (!isParticipant) {
      throw new ActivityAccessDeniedException("User is not participating in this activity");
    }
  }

  private Location toLocation(LocationDTO dto) {
    return new Location(cityRepository.findOrCreate(dto.city()), dto.latitude(), dto.longitude());
  }

  private ReprogramationRange toReprogramationRange(ReprogramationRangeDTO dto) {
    return new ReprogramationRange(dto.maxDays(), dto.initialHour(), dto.finalHour());
  }

  private ReprogramationRangeDTO toReprogramationRangeDTO(ReprogramationRange range) {
    return new ReprogramationRangeDTO(
        range.getMaxDays(), range.getInitialHour(), range.getFinalHour());
  }

  private List<WeatherCondition> toWeatherConditions(WeatherConditionsDTO conditions) {
    List<WeatherCondition> weatherConditions = new ArrayList<>();

    Integer maxRainProbability = conditions.maxRainProbability();
    Integer minTemperature = conditions.minTemperature();
    Integer maxTemperature = conditions.maxTemperature();
    Double maxWindSpeed = conditions.maxWindSpeed();

    if (maxRainProbability != null) {
      weatherConditions.add(new MaxRainProbabilityCondition(maxRainProbability));
    }

    if (minTemperature != null && maxTemperature != null) {
      weatherConditions.add(new TemperatureRangeCondition(minTemperature, maxTemperature));
    }

    if (maxWindSpeed != null) {
      weatherConditions.add(new MaxWindCondition(maxWindSpeed));
    }

    return weatherConditions;
  }

  private ActivityResponse toResponse(Activity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getTitle(),
        activity.getDescription(),
        activity.getType(),
        toLocationDTO(activity.getLocation()),
        activity.getDateTime(),
        activity.getAvailability(),
        activity.getMinParticipants(),
        activity.getMaxParticipants(),
        activity.getParticipants().size(),
        toParticipantsDTO(activity.getParticipants()),
        toWeatherConditionsDTO(activity.getWeatherConditions()),
        activity.getAnticipationWindow(),
        toReprogramationRangeDTO(activity.getReprogramationRange()),
        activity.getStatus(),
        activity.getImageKeys().stream()
            .map(key -> imageStorage.signedReadUrl(key, IMAGE_URL_VALIDITY).toString())
            .toList());
  }

  private LocationDTO toLocationDTO(Location location) {
    return new LocationDTO(
        location.city() == null ? null : location.city().name(),
        location.latitude(),
        location.longitude());
  }

  private WeatherConditionsDTO toWeatherConditionsDTO(List<WeatherCondition> conditions) {
    Integer maxRainProbability = null;
    Integer minTemperature = null;
    Integer maxTemperature = null;
    Double maxWindSpeed = null;

    for (WeatherCondition condition : conditions) {
      if (condition instanceof MaxRainProbabilityCondition rainCondition) {
        maxRainProbability = rainCondition.getMaxProbability();
      } else if (condition instanceof TemperatureRangeCondition temperatureCondition) {
        minTemperature = temperatureCondition.getMinTemperature();
        maxTemperature = temperatureCondition.getMaxTemperature();
      } else if (condition instanceof MaxWindCondition windCondition) {
        maxWindSpeed = windCondition.getMaxWindSpeed();
      }
    }

    return new WeatherConditionsDTO(
        maxRainProbability, minTemperature, maxTemperature, maxWindSpeed);
  }

  private List<ParticipantDTO> toParticipantsDTO(
      List<com.solnotfound.entity.user.User> participants) {
    return participants.stream()
        .map(participant -> new ParticipantDTO(participant.getId()))
        .toList();
  }

  private ActivityWeatherResponse toWeatherResponse(
      Activity activity, WeatherForecast currentWeather, WeatherForecast activityForecast) {
    return new ActivityWeatherResponse(
        activity.getId(),
        toLocationDTO(activity.getLocation()),
        activity.getDateTime(),
        toWeatherForecastDTO(currentWeather),
        toWeatherForecastDTO(activityForecast));
  }

  private WeatherForecastDTO toWeatherForecastDTO(WeatherForecast weatherForecast) {
    return new WeatherForecastDTO(
        weatherForecast.getDateTime(),
        weatherForecast.getTemperature(),
        weatherForecast.getChanceOfRain(),
        weatherForecast.getWindSpeed());
  }

  private Activity findActivityOrThrow(String activityId) {
    Activity activity = activityRepository.findById(activityId);

    if (activity == null) {
      throw new ActivityNotFoundException("Activity not found: " + activityId);
    }

    return activity;
  }
}
