package com.solnotfound.service;

import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.MaxRainProbabilityCondition;
import com.solnotfound.entity.MaxWindCondition;
import com.solnotfound.entity.TemperatureRangeCondition;
import com.solnotfound.entity.WeatherCondition;
import com.solnotfound.exception.InvalidActivityException;
import com.solnotfound.repository.ActivityRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
  private final ActivityRepository activityRepository;

  public ActivityService(ActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  public ActivityResponse create(CreateActivityRequest request) {
    validate(request);

    Activity activity = new Activity();
    activity.setId(UUID.randomUUID().toString());
    activity.setTitle(request.title());
    activity.setDescription(request.description());
    activity.setType(request.type());
    activity.setLocation(toLocation(request.location()));
    activity.setDateTime(request.dateTime());
    activity.setMinParticipants(request.minParticipants());
    activity.setMaxParticipants(request.maxParticipants());
    activity.setWeatherConditions(toWeatherConditions(request.weatherConditions()));
    activity.setAnticipationWindow(request.anticipationWindow());

    activityRepository.save(activity);

    return toResponse(activity);
  }

  public List<ActivityResponse> getAll() {
    return activityRepository.findAll().stream().map(this::toResponse).toList();
  }

  public ActivityResponse getById(String id) {
    Activity activity = activityRepository.findById(id);
    if (activity == null) {
      return null;
    }
    return toResponse(activity);
  }

  private void validate(CreateActivityRequest request) {
    if (request.minParticipants() > request.maxParticipants()) {
      throw new InvalidActivityException("Minimum participants cannot exceed maximum participants");
    }

    validateLocation(request.location());
    validateWeatherConditions(request.weatherConditions());
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

  private Location toLocation(LocationDTO dto) {
    return new Location(dto.city(), dto.latitude(), dto.longitude());
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
        activity.getMinParticipants(),
        activity.getMaxParticipants(),
        toWeatherConditionsDTO(activity.getWeatherConditions()));
  }

  private LocationDTO toLocationDTO(Location location) {
    return new LocationDTO(location.city(), location.latitude(), location.longitude());
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
}
