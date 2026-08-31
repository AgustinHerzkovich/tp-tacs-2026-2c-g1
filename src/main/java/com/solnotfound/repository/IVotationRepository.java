package com.solnotfound.repository;

import com.solnotfound.entity.Votation;
import java.time.LocalDateTime;
import java.util.List;

public interface IVotationRepository {

  Votation findById(String id);

  List<Votation> findAll();

  Votation save(Votation votation);

  List<Votation> findByActivityIds(List<String> activityIds);

  Votation findActiveByActivityId(String activityId);

  List<Votation> findActiveDueToClose(LocalDateTime now);
}
