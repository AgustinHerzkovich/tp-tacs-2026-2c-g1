package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solnotfound.entity.statistics.StatisticsEvent;
import com.solnotfound.entity.statistics.StatisticsEventType;
import com.solnotfound.repository.IStatisticsEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

class StatisticsEventRecorderTest {

  @Test
  void aProviderEventThatCannotBeStoredNeverBreaksTheWeatherLookup() {
    IStatisticsEventRepository repository = mock(IStatisticsEventRepository.class);
    when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("write timeout"));
    StatisticsEventRecorder recorder = new StatisticsEventRecorder(repository);

    assertThatCode(
            () -> {
              recorder.recordProviderCall("Legacy", false, 1500, "TIMEOUT");
              recorder.recordProviderRefusal("Legacy", "CIRCUIT_OPEN");
              recorder.recordFallback("Legacy");
              recorder.recordWeatherRequest(true, 120);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void countsRefusalsAndFallbacksInMemoryAndStoresOneEventPerKindOnFlush() {
    IStatisticsEventRepository repository = mock(IStatisticsEventRepository.class);
    StatisticsEventRecorder recorder = new StatisticsEventRecorder(repository);

    for (int index = 0; index < 3; index++) {
      recorder.recordProviderRefusal("Legacy", "CIRCUIT_OPEN");
      recorder.recordFallback("Legacy");
    }
    recorder.recordProviderRefusal("Legacy", "OVERLOADED");
    verify(repository, never()).save(any());

    recorder.flushProviderCounters();

    ArgumentCaptor<StatisticsEvent> saved = ArgumentCaptor.forClass(StatisticsEvent.class);
    verify(repository, times(3)).save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(StatisticsEvent::type, StatisticsEvent::outcome, StatisticsEvent::occurrences)
        .containsExactlyInAnyOrder(
            org.assertj.core.api.Assertions.tuple(
                StatisticsEventType.WEATHER_PROVIDER_REFUSED, "CIRCUIT_OPEN", 3L),
            org.assertj.core.api.Assertions.tuple(
                StatisticsEventType.WEATHER_PROVIDER_REFUSED, "OVERLOADED", 1L),
            org.assertj.core.api.Assertions.tuple(
                StatisticsEventType.WEATHER_PROVIDER_FALLBACK, null, 3L));

    recorder.flushProviderCounters();
    verify(repository, times(3)).save(any());
  }

  @Test
  void keepsTheCountsThatCouldNotBeStoredForTheNextFlush() {
    IStatisticsEventRepository repository = mock(IStatisticsEventRepository.class);
    when(repository.save(any()))
        .thenThrow(new DataAccessResourceFailureException("write timeout"))
        .thenAnswer(invocation -> invocation.getArgument(0));
    StatisticsEventRecorder recorder = new StatisticsEventRecorder(repository);
    recorder.recordFallback("Legacy");
    recorder.recordFallback("Legacy");

    recorder.flushProviderCounters();
    recorder.recordFallback("Legacy");
    recorder.flushProviderCounters();

    ArgumentCaptor<StatisticsEvent> saved = ArgumentCaptor.forClass(StatisticsEvent.class);
    verify(repository, times(2)).save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(StatisticsEvent::occurrences)
        .containsExactly(2L, 3L);
  }
}
