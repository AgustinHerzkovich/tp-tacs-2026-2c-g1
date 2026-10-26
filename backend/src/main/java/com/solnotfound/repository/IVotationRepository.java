package com.solnotfound.repository;

import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.votation.Votation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IVotationRepository {

  Votation findById(String id);

  List<Votation> findAll();

  /**
   * Stores a votation. An existing votation is only replaced when it was not stored by anyone else
   * since this instance was read; on success its version is incremented.
   *
   * @param votation votation to insert or replace
   * @return the stored votation
   * @throws org.springframework.dao.OptimisticLockingFailureException when the stored votation
   *     changed after this instance was read; the caller must read it again and reapply its change
   */
  Votation save(Votation votation);

  /**
   * Removes every vote a user cast in the active votations of an activity, in a single atomic
   * update per votation. Does nothing when the user has not voted.
   *
   * @param activityId activity whose active votations are updated
   * @param userId user whose votes are removed
   */
  void removeVotes(String activityId, String userId);

  List<Votation> findByActivityIds(List<String> activityIds);

  /**
   * Returns one page of the votations of the given activities that match the filter, newest first.
   *
   * @param activityIds activities to search in; an empty list yields an empty page
   * @param filter optional status and vote filters ({@code activityId} is applied by the caller)
   * @param userId user whose votes {@code filter.votedByMe()} refers to
   * @param pageable requested page; its sort is ignored in favor of newest first
   * @return matching votations
   */
  Page<Votation> search(
      List<String> activityIds, VotationFilterDTO filter, String userId, Pageable pageable);

  Votation findActiveByActivityId(String activityId);

  List<Votation> findActiveDueToClose(LocalDateTime now);
}
