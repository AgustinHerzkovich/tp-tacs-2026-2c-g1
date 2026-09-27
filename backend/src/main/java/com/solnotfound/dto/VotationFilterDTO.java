package com.solnotfound.dto;

import com.solnotfound.entity.votation.VotationStatus;

/**
 * Optional criteria for listing the current user's votations.
 *
 * @param status keeps only votations in this status; {@code null} means any status
 * @param activityId keeps only votations of this activity; {@code null} means any activity
 * @param votedByMe {@code true} keeps votations the user voted in, {@code false} those the user has
 *     not voted in yet, {@code null} ignores votes
 */
public record VotationFilterDTO(VotationStatus status, String activityId, Boolean votedByMe) {}
