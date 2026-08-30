package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.ReprogramationRange;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.repository.VotationRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VotationServiceTest {

  private ActivityRepository activityRepository;
  private VotationRepository votationRepository;
  private IWeatherAdapter weatherAdapter;
  private IBadWeatherChecker badWeatherChecker;
  private VotationService service;

  @BeforeEach
  void setUp() {
    activityRepository = new ActivityRepository();
    votationRepository = new VotationRepository();
    weatherAdapter = mock(IWeatherAdapter.class);
    badWeatherChecker = mock(IBadWeatherChecker.class);
    service =
        new VotationService(
            votationRepository, activityRepository, weatherAdapter, badWeatherChecker);
  }

  @Test
  void listsVotationsAccessibleToOrganizerOrParticipant() {
    Activity organized = activity("a-1", "user-1", List.of("user-2"));
    Activity joined = activity("a-2", "user-3", List.of("user-1"));
    activityRepository.save(organized);
    activityRepository.save(joined);
    votationRepository.save(votation("v-1", "a-1"));
    votationRepository.save(votation("v-2", "a-2"));
    votationRepository.save(votation("v-3", "missing"));

    assertThat(service.getByOrganizerOrParticipantId("user-1"))
        .extracting(dto -> dto.id())
        .containsExactlyInAnyOrder("v-1", "v-2");
  }

  @Test
  void organizerReplacesOptionsAndVotesAreReset() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    Votation votation = votation("v-1", "a-1");
    votationRepository.save(votation);
    LocalDateTime candidate = activity.getDateTime().plusDays(1).withHour(12);
    when(weatherAdapter.getFutureClimate(any(Location.class), any(LocalDateTime.class)))
        .thenReturn(mock(WeatherForecast.class));
    when(badWeatherChecker.isBadWeatherForActivity(any(), any())).thenReturn(false);

    var result =
        service.updateVotationOptions(
            "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "organizer");

    assertThat(result.options()).hasSize(1);
    assertThat(result.options().getFirst().users()).isEmpty();
    assertThat(votationRepository.findById("v-1").getOptions().getFirst().getUsers()).isEmpty();
  }

  @Test
  void rejectsNonOrganizerAndInvalidWeather() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    votationRepository.save(votation("v-1", "a-1"));
    LocalDateTime candidate = activity.getDateTime().plusDays(1).withHour(12);

    assertThatThrownBy(
            () ->
                service.updateVotationOptions(
                    "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "participant"))
        .isInstanceOf(AccessDeniedException.class);

    when(weatherAdapter.getFutureClimate(any(Location.class), any(LocalDateTime.class)))
        .thenReturn(mock(WeatherForecast.class));
    when(badWeatherChecker.isBadWeatherForActivity(any(), any())).thenReturn(true);
    assertThatThrownBy(
            () ->
                service.updateVotationOptions(
                    "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "organizer"))
        .isInstanceOf(InvalidVotationOptionsException.class);
  }

  private Activity activity(String id, String organizerId, List<String> participantIds) {
    Activity activity = new Activity();
    activity.setId(id);
    activity.setOrganizer(User.withId(organizerId));
    activity.setParticipants(participantIds.stream().map(User::withId).toList());
    activity.setLocation(new Location(null, -34.6, -58.4));
    activity.setDateTime(LocalDateTime.now().plusDays(1).withHour(10));
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(18, 0)));
    return activity;
  }

  private Votation votation(String id, String activityId) {
    Votation votation = new Votation();
    votation.setId(id);
    votation.setActivityId(activityId);
    votation.setCreationDate(LocalDateTime.now());
    votation.setStatus(VotationStatus.ACTIVE);
    return votation;
  }
}
