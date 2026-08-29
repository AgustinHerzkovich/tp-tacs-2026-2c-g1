package com.solnotfound.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.ActivityDTO;
import com.solnotfound.dto.LocationDTO;
import com.solnotfound.dto.ReprogramationRangeDTO;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.dto.WeatherConditionsDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.MaxRainProbabilityCondition;
import com.solnotfound.entity.MaxWindCondition;
import com.solnotfound.entity.ReprogramationRange;
import com.solnotfound.entity.TemperatureRangeCondition;
import com.solnotfound.entity.User;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityMapperTest {

  @Test
  void mapsActivityToDTO() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 9, 1, 10, 0);
    Activity activity = activity(dateTime);

    ActivityDTO dto = ActivityMapper.toDTO(activity);

    assertThat(dto.id()).isEqualTo("activity-1");
    assertThat(dto.title()).isEqualTo("Football match");
    assertThat(dto.description()).isEqualTo("Friendly match");
    assertThat(dto.type()).isEqualTo(ActivityType.OUTDOOR);
    assertThat(dto.location()).isEqualTo(new LocationDTO("Buenos Aires", -34.6, -58.4));
    assertThat(dto.dateTime()).isEqualTo(dateTime);
    assertThat(dto.availability()).isTrue();
    assertThat(dto.minParticipants()).isEqualTo(10);
    assertThat(dto.maxParticipants()).isEqualTo(20);
    assertThat(dto.weatherConditions()).isEqualTo(new WeatherConditionsDTO(30, 10, 28, 25.0));
    assertThat(dto.anticipationWindow()).isEqualTo(15);
    assertThat(dto.reprogramationRange())
        .isEqualTo(new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
    assertThat(dto.organizer()).isEqualTo(new UserDTO("1", "Organizer", "organizer@example.com"));
    assertThat(dto.participants())
        .containsExactly(new UserDTO("2", "Participant", "user@example.com"));
  }

  @Test
  void mapsDTOToActivity() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 9, 1, 10, 0);
    ActivityDTO dto =
        new ActivityDTO(
            "activity-1",
            "Football match",
            "Friendly match",
            ActivityType.OUTDOOR,
            new LocationDTO("Buenos Aires", -34.6, -58.4),
            dateTime,
            true,
            10,
            20,
            new WeatherConditionsDTO(30, 10, 28, 25.0),
            15,
            new ReprogramationRangeDTO(3, LocalTime.of(10, 0), LocalTime.of(20, 0)),
            new UserDTO("1", "Organizer", "organizer@example.com"),
            List.of(new UserDTO("2", "Participant", "user@example.com")));

    Activity activity = ActivityMapper.toEntity(dto);

    assertThat(activity.getId()).isEqualTo("activity-1");
    assertThat(activity.getTitle()).isEqualTo("Football match");
    assertThat(activity.getDescription()).isEqualTo("Friendly match");
    assertThat(activity.getType()).isEqualTo(ActivityType.OUTDOOR);
    assertThat(activity.getLocation()).isEqualTo(new Location("Buenos Aires", -34.6, -58.4));
    assertThat(activity.getDateTime()).isEqualTo(dateTime);
    assertThat(activity.getAvailability()).isTrue();
    assertThat(activity.getMinParticipants()).isEqualTo(10);
    assertThat(activity.getMaxParticipants()).isEqualTo(20);
    assertThat(activity.getWeatherConditions())
        .hasExactlyElementsOfTypes(
            MaxRainProbabilityCondition.class,
            TemperatureRangeCondition.class,
            MaxWindCondition.class);
    assertThat(activity.getAnticipationWindow()).isEqualTo(15);
    assertThat(activity.getReprogramationRange().getMaxDays()).isEqualTo(3);
    assertThat(activity.getOrganizer().getId()).isEqualTo("1");
    assertThat(activity.getParticipants()).extracting(User::getId).containsExactly("2");
  }

  @Test
  void mapsNullValues() {
    assertThat(ActivityMapper.toDTO(null)).isNull();
    assertThat(ActivityMapper.toEntity(null)).isNull();
  }

  private Activity activity(LocalDateTime dateTime) {
    Activity activity = new Activity();
    activity.setId("activity-1");
    activity.setTitle("Football match");
    activity.setDescription("Friendly match");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location("Buenos Aires", -34.6, -58.4));
    activity.setDateTime(dateTime);
    activity.setAvailability(true);
    activity.setMinParticipants(10);
    activity.setMaxParticipants(20);
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(30),
            new TemperatureRangeCondition(10, 28),
            new MaxWindCondition(25.0)));
    activity.setAnticipationWindow(15);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
    activity.setOrganizer(user("1", "Organizer", "organizer@example.com"));
    activity.setParticipants(List.of(user("2", "Participant", "user@example.com")));
    return activity;
  }

  private User user(String id, String name, String email) {
    User user = new User();
    user.setId(id);
    user.setName(name);
    user.setEmail(email);
    return user;
  }
}
