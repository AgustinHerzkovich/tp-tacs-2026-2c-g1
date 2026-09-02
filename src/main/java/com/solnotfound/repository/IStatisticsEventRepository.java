package com.solnotfound.repository;

import com.solnotfound.entity.statistics.StatisticsEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IStatisticsEventRepository extends MongoRepository<StatisticsEvent, String> {

  List<StatisticsEvent> findByOccurredAtBetween(Instant from, Instant to);

  default List<StatisticsEvent> findOccurredBetween(Instant from, Instant to) {
    return findByOccurredAtBetween(from, to);
  }
}
