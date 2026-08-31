package com.solnotfound.service;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.dto.UpdateVotationSettingsRequest;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationOption;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.exception.InvalidVotationSettingsException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.exception.WeatherUnavailableException;
import com.solnotfound.mapper.VotationMapper;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring injects shared application collaborators")
public class VotationService {

  private final IVotationRepository votationRepository;
  private final IActivityRepository activityRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;

  public VotationService(
      IVotationRepository votationRepository,
      IActivityRepository activityRepository,
      IWeatherAdapter weatherAdapter,
      IBadWeatherChecker badWeatherChecker) {
    this.votationRepository = votationRepository;
    this.activityRepository = activityRepository;
    this.weatherAdapter = weatherAdapter;
    this.badWeatherChecker = badWeatherChecker;
  }

  public List<VotationDTO> getByOrganizerOrParticipantId(String userId) {
    List<String> activityIds =
        Stream.concat(
                activityRepository.findActivitiesByOrganizerId(userId).stream(),
                activityRepository.findActivitiesByParticipantId(userId).stream())
            .map(Activity::getId)
            .distinct()
            .toList();
    List<Votation> votations = votationRepository.findByActivityIds(activityIds);
    return votations.stream().map(VotationMapper::toDTO).toList();
  }

  public VotationDTO updateVotationOptions(
      String votationId, UpdateVotationOptionsRequest request, String userId) {
    Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      throw new ResourceNotFoundException("Votation not found: " + votationId);
    }

    Activity activity = activityRepository.findById(votation.getActivityId());
    if (activity == null) {
      throw new ResourceNotFoundException("Activity not found: " + votation.getActivityId());
    }
    if (activity.getOrganizer() == null || !userId.equals(activity.getOrganizer().getId())) {
      throw new AccessDeniedException("Only the activity organizer can update votation options");
    }
    if (votation.getStatus() != com.solnotfound.entity.VotationStatus.ACTIVE) {
      throw new InvalidVotationOptionsException(request.dates());
    }
    if (new HashSet<>(request.dates()).size() != request.dates().size()) {
      throw new InvalidVotationOptionsException(request.dates());
    }

    List<LocalDateTime> outOfRangeOptions =
        request.dates().stream()
            .filter(
                date ->
                    !activity.getReprogramationRange().isWithinRange(activity.getDateTime(), date))
            .toList();
    List<LocalDateTime> datesWithinRange =
        request.dates().stream().filter(date -> !outOfRangeOptions.contains(date)).toList();
    List<WeatherForecast> forecasts =
        weatherAdapter.getForecastRange(activity.getLocation(), datesWithinRange);
    if (forecasts.size() != datesWithinRange.size()) {
      throw new WeatherUnavailableException("Provider returned an incomplete forecast range");
    }
    List<LocalDateTime> invalidOptions = new ArrayList<>(outOfRangeOptions);
    for (int index = 0; index < forecasts.size(); index++) {
      if (badWeatherChecker.isBadWeatherForActivity(forecasts.get(index), activity)) {
        invalidOptions.add(datesWithinRange.get(index));
      }
    }
    if (!invalidOptions.isEmpty()) {
      throw new InvalidVotationOptionsException(invalidOptions);
    }

    List<VotationOption> existingOptions = votation.getOptions();
    List<VotationOption> newOptions = new ArrayList<>();
    for (LocalDateTime date : request.dates()) {
      VotationOption option =
          existingOptions.stream()
              .filter(existing -> existing.getDateTime().equals(date))
              .findFirst()
              .orElseGet(
                  () -> {
                    VotationOption created = new VotationOption();
                    created.setDateTime(date);
                    created.setUsers(List.of());
                    return created;
                  });
      newOptions.add(option);
    }
    votation.setOptions(newOptions);
    votationRepository.save(votation);

    return VotationMapper.toDTO(votation);
  }

  /**
   * Updates quorum and remaining duration for an active votation. The resulting closing date must
   * be in the future and before every proposed alternative.
   *
   * @param votationId votation identifier
   * @param request new quorum and duration
   * @param userId authenticated organizer identifier
   * @return the updated votation
   * @throws ResourceNotFoundException when the votation or activity does not exist
   * @throws AccessDeniedException when the user is not the organizer or the votation is closed
   * @throws InvalidVotationSettingsException when duration or closing date is invalid
   */
  public VotationDTO updateVotationSettings(
      String votationId, UpdateVotationSettingsRequest request, String userId) {
    Votation votation = findVotation(votationId);
    Activity activity = findActivity(votation);
    requireOrganizer(activity, userId);
    if (votation.getStatus() != VotationStatus.ACTIVE) {
      throw new AccessDeniedException("Votation already closed: " + votationId);
    }
    Duration duration = request.duration();
    if (duration.isZero() || duration.isNegative()) {
      throw new InvalidVotationSettingsException("Duration must be greater than zero");
    }
    LocalDateTime closingDate = LocalDateTime.now().plus(duration);
    LocalDateTime earliestOption =
        votation.getOptions().stream()
            .map(VotationOption::getDateTime)
            .min(LocalDateTime::compareTo)
            .orElseThrow(
                () -> new InvalidVotationSettingsException("Votation must have alternatives"));
    if (!closingDate.isBefore(earliestOption)) {
      throw new InvalidVotationSettingsException(
          "Votation must close before its earliest alternative");
    }
    votation.setMinQuorum(request.minQuorum());
    votation.setClosingDate(closingDate);
    votationRepository.save(votation);
    return VotationMapper.toDTO(votation);
  }

  private Votation findVotation(String votationId) {
    Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      throw new ResourceNotFoundException("Votation not found: " + votationId);
    }
    return votation;
  }

  private Activity findActivity(Votation votation) {
    Activity activity = activityRepository.findById(votation.getActivityId());
    if (activity == null) {
      throw new ResourceNotFoundException("Activity not found: " + votation.getActivityId());
    }
    return activity;
  }

  private void requireOrganizer(Activity activity, String userId) {
    if (activity.getOrganizer() == null || !userId.equals(activity.getOrganizer().getId())) {
      throw new AccessDeniedException("Only the activity organizer can update the votation");
    }
  }

  /**
   * Registers or changes a user's vote in an active votation. Repeating the current choice is
   * idempotent and each user remains associated with at most one option.
   *
   * @param votationId votation identifier
   * @param userId authenticated user identifier
   * @param vote selected option date and time
   * @return the votation with its updated partial result
   * @throws ResourceNotFoundException when the votation, activity, option, or user does not exist
   * @throws AccessDeniedException when the votation is closed
   */
  public VotationDTO vote(String votationId, String userId, LocalDateTime vote) {
    final Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      throw new ResourceNotFoundException("Votation not found: " + votationId);
    }
    if (!votation.isAnOption(vote)) {
      throw new ResourceNotFoundException("Option not found: " + vote);
    }
    final Activity activity = activityRepository.findById(votation.getActivityId());
    if (activity == null) {
      throw new ResourceNotFoundException("Activity not found: " + votation.getActivityId());
    }
    final User user =
        activity
            .findOrganizerOrParticipant(userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "User doesn't belong to this activity: " + userId));
    if (votation.getStatus() != VotationStatus.ACTIVE) {
      throw new AccessDeniedException("Votation already closed: " + votationId);
    }
    final Optional<LocalDateTime> currentVote = votation.getVoteByUser(user);
    if (currentVote.isPresent() && !currentVote.get().equals(vote)) {
      votation.unvote(currentVote.get(), user);
    }
    if (currentVote.isEmpty() || !currentVote.get().equals(vote)) {
      votation.vote(vote, user);
    }
    votationRepository.save(votation);
    return VotationMapper.toDTO(votation);
  }
}
