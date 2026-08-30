package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.*;
import com.solnotfound.entity.notifications.BadWeatherAlertNotificationType;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ActivityAnticipationCheckSchedulerTest {

  @Mock private IActivityRepository activityRepository;

  @Mock private IWeatherAdapter weatherAdapter;

  @Mock private IBadWeatherChecker badWeatherChecker;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private IVotationRepository votationRepository;

  @InjectMocks private ActivityAnticipationCheckScheduler scheduler;

  private Activity activityToCheck;
  private Activity activityNotToCheck;
  private Location location;
  private LocalDateTime dateTime;
  private WeatherForecast weather;
  private ReprogramationRange range;

  @BeforeEach
  void setUp() {
    scheduler =
        new ActivityAnticipationCheckScheduler(
            activityRepository,
            votationRepository,
            weatherAdapter,
            badWeatherChecker,
            eventPublisher);
    location = new Location(new City("ba", "Buenos Aires"), -34.6037, -58.3816);
    dateTime = LocalDateTime.now().plusHours(2);
    weather = mock(WeatherForecast.class);

    activityToCheck = mock(Activity.class);
    lenient().when(activityToCheck.isTimeToCheckWeatherConditions()).thenReturn(true);
    lenient().when(activityToCheck.getLocation()).thenReturn(location);
    lenient().when(activityToCheck.getDateTime()).thenReturn(dateTime);
    lenient().when(activityToCheck.getId()).thenReturn("activity-1");

    range = mock(ReprogramationRange.class);
    lenient().when(range.getMaxDays()).thenReturn(1);
    lenient().when(range.getInitialHour()).thenReturn(LocalTime.of(10, 0));
    lenient().when(activityToCheck.getReprogramationRange()).thenReturn(range);
    lenient().when(votationRepository.findActiveByActivityId("activity-1")).thenReturn(null);

    activityNotToCheck = mock(Activity.class);
    lenient().when(activityNotToCheck.isTimeToCheckWeatherConditions()).thenReturn(false);
  }

  @Test
  void onlyChecksWeatherForActivitiesThatAreDueForCheck() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck, activityNotToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
    verify(weatherAdapter, times(1)).getFutureClimate(locationCaptor.capture(), eq(dateTime));

    assertThat(locationCaptor.getAllValues()).containsExactly(location);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void notifiesWhenWeatherIsBad() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(true);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<ActivityNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    ActivityNotificationEvent firedEvent = eventCaptor.getValue();
    assertThat(firedEvent.activityId()).isEqualTo(activityToCheck.getId());
    assertThat(firedEvent.type()).isInstanceOf(BadWeatherAlertNotificationType.class);
  }

  @Test
  void doesNotNotifyWhenWeatherIsGood() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(false);

    scheduler.checkActivitiesClimate();

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void skipsActivitiesThatAreNotDueForCheck() {
    when(activityRepository.findActive()).thenReturn(List.of(activityNotToCheck));

    scheduler.checkActivitiesClimate();

    verifyNoInteractions(weatherAdapter, badWeatherChecker, eventPublisher);
  }

  @Test
  void continuesProcessingRemainingActivitiesWhenWeatherAdapterThrows() throws Exception {
    Activity anotherActivity = mock(Activity.class);
    Location anotherLocation = new Location(new City("cordoba", "Cordoba"), -31.4201, -64.1888);
    LocalDateTime anotherDateTime = LocalDateTime.now().plusHours(3);
    WeatherForecast anotherWeather = mock(WeatherForecast.class);

    when(anotherActivity.isTimeToCheckWeatherConditions()).thenReturn(true);
    when(anotherActivity.getLocation()).thenReturn(anotherLocation);
    when(anotherActivity.getDateTime()).thenReturn(anotherDateTime);
    when(anotherActivity.getReprogramationRange()).thenReturn(range);

    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck, anotherActivity));

    // First activity throws while fetching the weather
    when(weatherAdapter.getFutureClimate(location, dateTime))
        .thenThrow(new RuntimeException("Error fetching weather"));

    // Second activity should still be processed normally
    when(weatherAdapter.getFutureClimate(anotherLocation, anotherDateTime))
        .thenReturn(anotherWeather);
    when(badWeatherChecker.isBadWeatherForActivity(anotherWeather, anotherActivity))
        .thenReturn(true, false);
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<ActivityNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    ActivityNotificationEvent firedEvent = eventCaptor.getValue();
    assertThat(firedEvent.activityId()).isEqualTo(anotherActivity.getId());
    assertThat(firedEvent.type()).isInstanceOf(BadWeatherAlertNotificationType.class);
  }

  @Test
  void doesNothingWhenThereAreNoActivities() {
    when(activityRepository.findActive()).thenReturn(List.of());

    scheduler.checkActivitiesClimate();

    verifyNoInteractions(weatherAdapter, badWeatherChecker, eventPublisher);
  }

  @Test
  void retriesWeatherCheckOnNextSchedulerRunAfterPreviousFailure() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));

    when(weatherAdapter.getFutureClimate(location, dateTime))
        .thenThrow(
            new RuntimeException("Error fetching weather")) // first method call throws exception.
        .thenReturn(weather); // second method call returns weather.

    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, false);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck)))
        .thenReturn(true, false);

    // first scheduler run: the weather check fails, so no notification is sent
    scheduler.checkActivitiesClimate();
    verifyNoInteractions(eventPublisher);

    // --- Segunda corrida (1 hora después): como el chequeo anterior falló,
    // la actividad sigue siendo candidata (isTimeToCheckWeatherConditions() sigue en true) ---
    scheduler.checkActivitiesClimate();

    verify(weatherAdapter, times(2)).getFutureClimate(location, dateTime);

    ArgumentCaptor<ActivityNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ActivityNotificationEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    ActivityNotificationEvent firedEvent = eventCaptor.getValue();
    assertThat(firedEvent.activityId()).isEqualTo(activityToCheck.getId());
    assertThat(firedEvent.type()).isInstanceOf(BadWeatherAlertNotificationType.class);
  }

  @Test
  void persistsBadWeatherOutcomeBeforeNotificationDeliveryFails() {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, false);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck)))
        .thenReturn(true, false);
    doThrow(new RuntimeException("Notification failed"))
        .when(eventPublisher)
        .publishEvent(any(ActivityNotificationEvent.class));

    scheduler.checkActivitiesClimate();

    verify(activityToCheck).markWeatherChecked();
    verify(activityRepository).save(activityToCheck);
    verify(votationRepository).save(any(Votation.class));
    verify(activityToCheck).setStatus(ActivityStatus.PROPOSED);
  }

  @Test
  void opensActiveVotationWithGoodWeatherOptionsWhenNoneIsActive() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    WeatherForecast initialWeather = new WeatherForecast(1, dateTime, 20.0f, 90.0f, 10.0f);
    WeatherForecast candidateWeather =
        new WeatherForecast(2, dateTime.plusDays(1), 20.0f, 0.0f, 10.0f);
    when(weatherAdapter.getFutureClimate(eq(location), any(LocalDateTime.class)))
        .thenAnswer(
            invocation ->
                dateTime.equals(invocation.getArgument(1)) ? initialWeather : candidateWeather);
    when(badWeatherChecker.isBadWeatherForActivity(initialWeather, activityToCheck))
        .thenReturn(true);
    when(badWeatherChecker.isBadWeatherForActivity(candidateWeather, activityToCheck))
        .thenReturn(false);
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, true, false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Votation> votationCaptor = ArgumentCaptor.forClass(Votation.class);
    verify(votationRepository).save(votationCaptor.capture());
    Votation saved = votationCaptor.getValue();

    assertThat(saved.getStatus()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(saved.getActivityId()).isEqualTo("activity-1");
    assertThat(saved.getOptions()).allSatisfy(option -> assertThat(option.getUsers()).isEmpty());
    assertThat(saved.getOptions())
        .extracting(VotationOption::getDateTime)
        .containsExactly(
            dateTime.plusDays(1).withHour(10).withMinute(0).withSecond(0),
            dateTime.plusDays(1).withHour(11).withMinute(0).withSecond(0));
    verify(activityToCheck).setStatus(ActivityStatus.PROPOSED);
    verify(activityRepository).save(activityToCheck);
  }

  @Test
  void doesNotOpenVotationWhenAnActiveVotationAlreadyExists() throws Exception {
    Votation activeVotation = mock(Votation.class);
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(any(), any())).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck))).thenReturn(true);
    when(votationRepository.findActiveByActivityId("activity-1")).thenReturn(activeVotation);

    scheduler.checkActivitiesClimate();

    verify(eventPublisher).publishEvent(any(ActivityNotificationEvent.class));
    verify(votationRepository, never()).save(any());
  }

  @Test
  void onlyOffersSlotsWithGoodWeather() throws Exception {
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(any(), any())).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck)))
        .thenReturn(true, true, false);
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, true, false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Votation> votationCaptor = ArgumentCaptor.forClass(Votation.class);
    verify(votationRepository).save(votationCaptor.capture());
    Votation saved = votationCaptor.getValue();

    assertThat(saved.getOptions())
        .extracting(VotationOption::getDateTime)
        .containsExactly(dateTime.plusDays(1).withHour(11).withMinute(0).withSecond(0));
  }

  @Test
  void considersConfiguredReprogramationRangeWhenOpeningVotation() throws Exception {
    when(range.getMaxDays()).thenReturn(2);
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true, true, false, true, true, false);
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(any(), any())).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck)))
        .thenReturn(true, false, false, false, false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Votation> votationCaptor = ArgumentCaptor.forClass(Votation.class);
    verify(votationRepository).save(votationCaptor.capture());
    Votation saved = votationCaptor.getValue();

    assertThat(saved.getOptions())
        .extracting(VotationOption::getDateTime)
        .containsExactly(
            dateTime.plusDays(1).withHour(10).withMinute(0).withSecond(0),
            dateTime.plusDays(1).withHour(11).withMinute(0).withSecond(0),
            dateTime.plusDays(2).withHour(10).withMinute(0).withSecond(0),
            dateTime.plusDays(2).withHour(11).withMinute(0).withSecond(0));
  }

  @Test
  void cancelsActivityWithoutOpeningVotationWhenNoCandidateIsWithinRange() throws Exception {
    when(range.isWithinRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);
    when(activityRepository.findActive()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(any(), any())).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(any(), eq(activityToCheck))).thenReturn(true);

    scheduler.checkActivitiesClimate();

    verify(votationRepository, never()).save(any());
    verify(activityToCheck).setStatus(ActivityStatus.CANCELLED);
    verify(activityToCheck).markWeatherChecked();
    verify(activityRepository).save(activityToCheck);
  }
}
