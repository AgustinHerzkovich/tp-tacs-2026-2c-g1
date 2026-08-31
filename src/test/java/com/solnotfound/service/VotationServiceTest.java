package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.dto.UpdateVotationSettingsRequest;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.IBadWeatherChecker;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.exception.InvalidVotationSettingsException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.repository.VotationRepository;
import java.time.Duration;
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
    votationRepository.save(votation("v-1", organized));
    votationRepository.save(votation("v-2", joined));
    votationRepository.save(votation("v-3", "missing"));

    assertThat(service.getByOrganizerOrParticipantId("user-1"))
        .extracting(dto -> dto.id())
        .containsExactlyInAnyOrder("v-1", "v-2");
  }

  @Test
  void organizerReplacesOptionsAndNewOptionsHaveNoVotes() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    Votation votation = votation("v-1", "a-1");
    votation.setActivity(activity);
    votationRepository.save(votation);
    LocalDateTime candidate = activity.getDateTime().plusDays(1).withHour(12);
    when(weatherAdapter.getForecastRange(any(Location.class), anyList()))
        .thenReturn(List.of(mock(WeatherForecast.class)));
    when(badWeatherChecker.isBadWeatherForActivity(any(), any())).thenReturn(false);

    var result =
        service.updateVotationOptions(
            "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "organizer");

    assertThat(result.options()).hasSize(1);
    assertThat(result.options().getFirst().voteCount()).isZero();
    assertThat(votationRepository.findById("v-1").getOptions().getFirst().getUsers()).isEmpty();
  }

  @Test
  void editingOptionsKeepsVotesOnlyForRetainedAlternatives() {
    Activity activity = activity("a-1", "organizer", List.of("first", "second"));
    activityRepository.save(activity);
    LocalDateTime retained = activity.getDateTime().plusDays(1).withHour(12);
    LocalDateTime removed = activity.getDateTime().plusDays(2).withHour(12);
    LocalDateTime added = activity.getDateTime().plusDays(3).withHour(12);
    Votation votation = votationWithOptions("v-1", "a-1", retained, removed);
    votation.setActivity(activity);
    votation.getOptions().get(0).setUsers(List.of(activity.getParticipants().get(0)));
    votation.getOptions().get(1).setUsers(List.of(activity.getParticipants().get(1)));
    votationRepository.save(votation);
    when(weatherAdapter.getForecastRange(any(Location.class), anyList()))
        .thenReturn(List.of(mock(WeatherForecast.class), mock(WeatherForecast.class)));
    when(badWeatherChecker.isBadWeatherForActivity(any(), any())).thenReturn(false);

    service.updateVotationOptions(
        "v-1", new UpdateVotationOptionsRequest(List.of(retained, added)), "organizer");

    assertThat(votation.getOptions().get(0).getUsers())
        .containsExactly(activity.getParticipants().get(0));
    assertThat(votation.getOptions().get(1).getUsers()).isEmpty();
    assertThat(votation.getVoteByUser(activity.getParticipants().get(1))).isEmpty();
  }

  @Test
  void organizerUpdatesQuorumAndRemainingDuration() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    Votation votation = votationWithOptions("v-1", "a-1", LocalDateTime.now().plusHours(4));
    votation.setActivity(activity);
    votationRepository.save(votation);
    LocalDateTime beforeUpdate = LocalDateTime.now();

    service.updateVotationSettings(
        "v-1", new UpdateVotationSettingsRequest(0.75, Duration.ofHours(2)), "organizer");

    assertThat(votation.getMinQuorum()).isEqualTo(0.75);
    assertThat(votation.getClosingDate())
        .isBetween(beforeUpdate.plusHours(2), LocalDateTime.now().plusHours(2));
  }

  @Test
  void rejectsInvalidDurationOrClosingDateAndNonOrganizer() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    Votation votation = votationWithOptions("v-1", "a-1", LocalDateTime.now().plusHours(2));
    votation.setActivity(activity);
    votationRepository.save(votation);

    assertThatThrownBy(
            () ->
                service.updateVotationSettings(
                    "v-1", new UpdateVotationSettingsRequest(0.5, Duration.ZERO), "organizer"))
        .isInstanceOf(InvalidVotationSettingsException.class);
    assertThatThrownBy(
            () ->
                service.updateVotationSettings(
                    "v-1",
                    new UpdateVotationSettingsRequest(0.5, Duration.ofHours(2)),
                    "organizer"))
        .isInstanceOf(InvalidVotationSettingsException.class)
        .hasMessageContaining("earliest alternative");
    assertThatThrownBy(
            () ->
                service.updateVotationSettings(
                    "v-1",
                    new UpdateVotationSettingsRequest(0.5, Duration.ofHours(1)),
                    "participant"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void rejectsNonOrganizerAndInvalidWeather() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    Votation votation = votation("v-1", activity);
    votationRepository.save(votation);
    LocalDateTime candidate = activity.getDateTime().plusDays(1).withHour(12);

    assertThatThrownBy(
            () ->
                service.updateVotationOptions(
                    "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "participant"))
        .isInstanceOf(AccessDeniedException.class);

    when(weatherAdapter.getForecastRange(any(Location.class), anyList()))
        .thenReturn(List.of(mock(WeatherForecast.class)));
    when(badWeatherChecker.isBadWeatherForActivity(any(), any())).thenReturn(true);
    assertThatThrownBy(
            () ->
                service.updateVotationOptions(
                    "v-1", new UpdateVotationOptionsRequest(List.of(candidate)), "organizer"))
        .isInstanceOf(InvalidVotationOptionsException.class);
  }

  @Test
  void participantVotesAndReceivesPartialResult() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activity.getParticipants().getFirst().setName("Jane Doe");
    activityRepository.save(activity);
    LocalDateTime firstOption = activity.getDateTime().plusDays(1);
    Votation votation = votationWithOptions("v-1", "a-1", firstOption);
    votation.setActivity(activity);
    votationRepository.save(votation);

    var result = service.vote("v-1", "participant", firstOption);

    assertThat(result.options().getFirst().voteCount()).isEqualTo(1);
    assertThat(result.options().getFirst().voterNames()).containsExactly("Jane Doe");
    assertThat(votation.getOptions().getFirst().getUsers())
        .containsExactly(activity.getParticipants().getFirst());
  }

  @Test
  void repeatingSameVoteIsIdempotent() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    LocalDateTime firstOption = activity.getDateTime().plusDays(1);
    Votation votation = votationWithOptions("v-1", "a-1", firstOption);
    votation.setActivity(activity);
    votationRepository.save(votation);

    service.vote("v-1", "participant", firstOption);
    service.vote("v-1", "participant", firstOption);

    assertThat(votation.getOptions().getFirst().getUsers()).hasSize(1);
  }

  @Test
  void participantChangesVoteWithoutVotingTwice() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    LocalDateTime firstOption = activity.getDateTime().plusDays(1);
    LocalDateTime secondOption = activity.getDateTime().plusDays(2);
    Votation votation = votationWithOptions("v-1", "a-1", firstOption, secondOption);
    votation.setActivity(activity);
    votationRepository.save(votation);

    service.vote("v-1", "participant", firstOption);
    var result = service.vote("v-1", "participant", secondOption);

    assertThat(result.options().get(0).voteCount()).isZero();
    assertThat(result.options().get(1).voteCount()).isEqualTo(1);
    assertThat(votation.getOptions().get(0).getUsers()).isEmpty();
    assertThat(votation.getOptions().get(1).getUsers()).hasSize(1);
  }

  @Test
  void rejectsVoteWhenVotationActivityOptionOrUserDoesNotExist() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    LocalDateTime option = activity.getDateTime().plusDays(1);

    assertThatThrownBy(() -> service.vote("missing", "participant", option))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Votation");

    Votation votation = votationWithOptions("v-1", "missing", option);
    votation.setActivity(null);
    votationRepository.save(votation);
    assertThatThrownBy(() -> service.vote("v-1", "participant", option))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Activity");

    votation.setActivity(activity);
    assertThatThrownBy(() -> service.vote("v-1", "participant", option.plusHours(1)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Option");
    assertThatThrownBy(() -> service.vote("v-1", "outsider", option))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User");
  }

  @Test
  void rejectsVoteWhenVotationIsClosed() {
    Activity activity = activity("a-1", "organizer", List.of("participant"));
    activityRepository.save(activity);
    LocalDateTime option = activity.getDateTime().plusDays(1);
    Votation votation = votationWithOptions("v-1", "a-1", option);
    votation.setActivity(activity);
    votation.setStatus(VotationStatus.CLOSED);
    votationRepository.save(votation);

    assertThatThrownBy(() -> service.vote("v-1", "participant", option))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("closed");
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
    Activity activity = new Activity();
    activity.setId(activityId);
    votation.setActivity(activity);
    votation.setCreationDate(LocalDateTime.now());
    votation.setStatus(VotationStatus.ACTIVE);
    return votation;
  }

  private Votation votation(String id, Activity activity) {
    Votation votation = votation(id, activity.getId());
    votation.setActivity(activity);
    return votation;
  }

  private Votation votationWithOptions(String id, String activityId, LocalDateTime... optionDates) {
    Votation votation = votation(id, activityId);
    votation.setOptions(
        java.util.Arrays.stream(optionDates)
            .map(
                date -> {
                  VotationOption option = new VotationOption();
                  option.setDateTime(date);
                  return option;
                })
            .toList());
    return votation;
  }
}
