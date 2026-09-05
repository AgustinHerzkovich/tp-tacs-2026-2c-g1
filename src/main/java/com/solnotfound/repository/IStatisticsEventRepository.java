package com.solnotfound.repository;

import com.solnotfound.entity.statistics.StatisticsEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface IStatisticsEventRepository extends MongoRepository<StatisticsEvent, String> {

  @Query("{ 'occurredAt': { $gte: ?0, $lte: ?1 } }")
  List<StatisticsEvent> findOccurredBetween(Instant from, Instant to);
}
