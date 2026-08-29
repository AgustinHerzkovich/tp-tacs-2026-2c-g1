package com.solnotfound.mapper;

import com.solnotfound.dto.ActivityDTO;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.MaxRainProbabilityCondition;
import com.solnotfound.entity.MaxWindCondition;
import com.solnotfound.entity.ReprogramationRange;
import com.solnotfound.entity.TemperatureRangeCondition;
import com.solnotfound.entity.User;
import com.solnotfound.entity.WeatherCondition;
import java.util.ArrayList;
import java.util.List;

public final class ActivityMapper {

  private ActivityMapper() {}

  public static ActivityDTO toDTO(Activity activity) {
    if (activity == null) {
      return null;
    }

    return new ActivityDTO(
        activity.getId(),
        activity.getTitle(),
        activity.getDescription(),
        activity.getType(),
        toLocationDTO(activity.getLocation()),
        activity.getDateTime(),
        activity.getAvailability(),
        activity.getMinParticipants(),
        activity.getMaxParticipants(),
        toWeatherConditionsDTO(activity.getWeatherConditions()),
        activity.getAnticipationWindow(),
        toReprogramationRangeDTO(activity.getReprogramationRange()),
        UserMapper.toDTO(activity.getOrganizer()),
        toUserDTOs(activity.getParticipants()));
  }

  public static Activity toEntity(ActivityDTO activityDTO) {
    if (activityDTO == null) {
      return null;
    }

    Activity activity = new Activity();
    activity.setId(activityDTO.id());
    activity.setTitle(activityDTO.title());
    activity.setDescription(activityDTO.description());
    activity.setType(activityDTO.type());
    activity.setLocation(toLocation(activityDTO.location()));
    activity.setDateTime(activityDTO.dateTime());
    activity.setAvailability(activityDTO.availability());
    activity.setMinParticipants(activityDTO.minParticipants());
    activity.setMaxParticipants(activityDTO.maxParticipants());
    activity.setWeatherConditions(toWeatherConditions(activityDTO.weatherConditions()));
    activity.setAnticipationWindow(activityDTO.anticipationWindow());
    activity.setReprogramationRange(toReprogramationRange(activityDTO.reprogramationRange()));
    activity.setOrganizer(UserMapper.toEntity(activityDTO.organizer()));
    activity.setParticipants(toUsers(activityDTO.participants()));

    return activity;
  }

  private static LocationDTO toLocationDTO(Location location) {
    if (location == null) {
      return null;
    }

    return new LocationDTO(location.city(), location.latitude(), location.longitude());
  }

  private static Location toLocation(LocationDTO locationDTO) {
    if (locationDTO == null) {
      return null;
    }

    return new Location(locationDTO.city(), locationDTO.latitude(), locationDTO.longitude());
  }

  private static ReprogramationRangeDTO toReprogramationRangeDTO(ReprogramationRange range) {
    if (range == null) {
      return null;
    }

    return new ReprogramationRangeDTO(
        range.getMaxDays(), range.getInitialHour(), range.getFinalHour());
  }

  private static ReprogramationRange toReprogramationRange(ReprogramationRangeDTO rangeDTO) {
    if (rangeDTO == null) {
      return null;
    }

    return new ReprogramationRange(
        rangeDTO.maxDays(), rangeDTO.initialHour(), rangeDTO.finalHour());
  }

  private static WeatherConditionsDTO toWeatherConditionsDTO(List<WeatherCondition> conditions) {
    if (conditions == null) {
      return null;
    }

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

  private static List<WeatherCondition> toWeatherConditions(WeatherConditionsDTO conditionsDTO) {
    if (conditionsDTO == null) {
      return List.of();
    }

    List<WeatherCondition> weatherConditions = new ArrayList<>();

    if (conditionsDTO.maxRainProbability() != null) {
      weatherConditions.add(new MaxRainProbabilityCondition(conditionsDTO.maxRainProbability()));
    }

    if (conditionsDTO.minTemperature() != null && conditionsDTO.maxTemperature() != null) {
      weatherConditions.add(
          new TemperatureRangeCondition(
              conditionsDTO.minTemperature(), conditionsDTO.maxTemperature()));
    }

    if (conditionsDTO.maxWindSpeed() != null) {
      weatherConditions.add(new MaxWindCondition(conditionsDTO.maxWindSpeed()));
    }

    return weatherConditions;
  }

  private static List<UserDTO> toUserDTOs(List<User> users) {
    if (users == null) {
      return null;
    }

    return users.stream().map(UserMapper::toDTO).toList();
  }

  private static List<User> toUsers(List<UserDTO> users) {
    if (users == null) {
      return null;
    }

    return users.stream().map(UserMapper::toEntity).toList();
  }
}
