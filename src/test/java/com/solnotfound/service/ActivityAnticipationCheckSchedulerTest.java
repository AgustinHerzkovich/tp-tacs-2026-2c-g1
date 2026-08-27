package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.*;
import com.solnotfound.repository.ActivityRepository;
import com.solnotfound.service.schedulers.ActivityAnticipationCheckScheduler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityAnticipationCheckSchedulerTest {

  @Mock private ActivityRepository activityRepository;

  @Mock private IWeatherAdapter weatherAdapter;

  @Mock private IBadWeatherChecker badWeatherChecker;

  @Mock private INotificationFacade notificationFacade;

  @InjectMocks private ActivityAnticipationCheckScheduler scheduler;

  private Activity activityToCheck;
  private Activity activityNotToCheck;
  private Location location;
  private LocalDateTime dateTime;
  private WeatherForecast weather;

  @BeforeEach
  void setUp() {
    location = mock(Location.class);
    dateTime = LocalDateTime.now().plusHours(2);
    weather = mock(WeatherForecast.class);

    activityToCheck = mock(Activity.class);
    lenient().when(activityToCheck.isTimeToCheckWeatherConditions()).thenReturn(true);
    lenient().when(activityToCheck.getLocation()).thenReturn(location);
    lenient().when(activityToCheck.getDateTime()).thenReturn(dateTime);

    activityNotToCheck = mock(Activity.class);
    lenient().when(activityNotToCheck.isTimeToCheckWeatherConditions()).thenReturn(false);
  }

  @Test
  void onlyChecksWeatherForActivitiesThatAreDueForCheck() throws Exception {
    when(activityRepository.findAll()).thenReturn(List.of(activityToCheck, activityNotToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(false);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
    verify(weatherAdapter, times(1)).getFutureClimate(locationCaptor.capture(), eq(dateTime));

    assertThat(locationCaptor.getAllValues()).containsExactly(location);
    verifyNoInteractions(notificationFacade);
  }

  @Test
  void notifiesWhenWeatherIsBad() throws Exception {
    when(activityRepository.findAll()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(true);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
    ArgumentCaptor<WeatherForecast> weatherCaptor = ArgumentCaptor.forClass(WeatherForecast.class);
    verify(notificationFacade, times(1))
        .notifyBadWeather(activityCaptor.capture(), weatherCaptor.capture());

    assertThat(activityCaptor.getValue()).isEqualTo(activityToCheck);
    assertThat(weatherCaptor.getValue()).isEqualTo(weather);
  }

  @Test
  void doesNotNotifyWhenWeatherIsGood() throws Exception {
    when(activityRepository.findAll()).thenReturn(List.of(activityToCheck));
    when(weatherAdapter.getFutureClimate(location, dateTime)).thenReturn(weather);
    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(false);

    scheduler.checkActivitiesClimate();

    verify(notificationFacade, never()).notifyBadWeather(any(), any());
  }

  @Test
  void skipsActivitiesThatAreNotDueForCheck() {
    when(activityRepository.findAll()).thenReturn(List.of(activityNotToCheck));

    scheduler.checkActivitiesClimate();

    verifyNoInteractions(weatherAdapter);
    verifyNoInteractions(badWeatherChecker);
    verifyNoInteractions(notificationFacade);
  }

  @Test
  void continuesProcessingRemainingActivitiesWhenWeatherAdapterThrows() throws Exception {
    Activity anotherActivity = mock(Activity.class);
    Location anotherLocation = mock(Location.class);
    LocalDateTime anotherDateTime = LocalDateTime.now().plusHours(3);
    WeatherForecast anotherWeather = mock(WeatherForecast.class);

    when(anotherActivity.isTimeToCheckWeatherConditions()).thenReturn(true);
    when(anotherActivity.getLocation()).thenReturn(anotherLocation);
    when(anotherActivity.getDateTime()).thenReturn(anotherDateTime);

    when(activityRepository.findAll()).thenReturn(List.of(activityToCheck, anotherActivity));

    // First activity throws while fetching the weather
    when(weatherAdapter.getFutureClimate(location, dateTime))
        .thenThrow(new RuntimeException("Error fetching weather"));

    // Second activity should still be processed normally
    when(weatherAdapter.getFutureClimate(anotherLocation, anotherDateTime))
        .thenReturn(anotherWeather);
    when(badWeatherChecker.isBadWeatherForActivity(anotherWeather, anotherActivity))
        .thenReturn(true);

    scheduler.checkActivitiesClimate();

    ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
    verify(notificationFacade, times(1))
        .notifyBadWeather(activityCaptor.capture(), eq(anotherWeather));

    assertThat(activityCaptor.getValue()).isEqualTo(anotherActivity);
  }

  @Test
  void doesNothingWhenThereAreNoActivities() {
    when(activityRepository.findAll()).thenReturn(List.of());

    scheduler.checkActivitiesClimate();

    verifyNoInteractions(weatherAdapter, badWeatherChecker, notificationFacade);
  }

  @Test
  void retriesWeatherCheckOnNextSchedulerRunAfterPreviousFailure() throws Exception {
    when(activityRepository.findAll()).thenReturn(List.of(activityToCheck));

    when(weatherAdapter.getFutureClimate(location, dateTime))
        .thenThrow(
            new RuntimeException("Error fetching weather")) // first method call throws exception.
        .thenReturn(weather); // second method call returns weather.

    when(badWeatherChecker.isBadWeatherForActivity(weather, activityToCheck)).thenReturn(true);

    // first scheduler run: the weather check fails, so no notification is sent
    scheduler.checkActivitiesClimate();
    verifyNoInteractions(notificationFacade);

    // --- Segunda corrida (1 hora después): como el chequeo anterior falló,
    // la actividad sigue siendo candidata (isTimeToCheckWeatherConditions() sigue en true) ---
    scheduler.checkActivitiesClimate();

    verify(weatherAdapter, times(2)).getFutureClimate(location, dateTime);

    ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
    ArgumentCaptor<WeatherForecast> weatherCaptor = ArgumentCaptor.forClass(WeatherForecast.class);
    verify(notificationFacade, times(1))
        .notifyBadWeather(activityCaptor.capture(), weatherCaptor.capture());

    assertThat(activityCaptor.getValue()).isEqualTo(activityToCheck);
    assertThat(weatherCaptor.getValue()).isEqualTo(weather);
  }
}
