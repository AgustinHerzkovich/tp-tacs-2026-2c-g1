package com.solnotfound.repository;

import com.solnotfound.entity.votation.Votation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

interface MongoVotationRepository extends MongoRepository<Votation, String> {

  @Query("{ 'activity': { $in: ?0 } }")
  List<Votation> findByActivityIds(List<String> activityIds);

  @Query("{ 'activity': ?0, 'status': 'ACTIVE' }")
  Optional<Votation> findActiveByActivityId(String activityId);

  @Query("{ 'status': 'ACTIVE', 'closingDate': { $ne: null, $lte: ?0 } }")
  List<Votation> findActiveDueToClose(LocalDateTime now);
}
