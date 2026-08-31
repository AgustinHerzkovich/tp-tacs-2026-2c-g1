package com.solnotfound.repository;

import com.solnotfound.entity.statistics.StatisticsEvent;
import java.time.Instant;
import java.util.List;

public interface IStatisticsEventRepository {
  StatisticsEvent save(StatisticsEvent event);

  List<StatisticsEvent> findOccurredBetween(Instant from, Instant to);

  void deleteAll();
}
