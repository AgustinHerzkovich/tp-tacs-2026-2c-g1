package com.solnotfound.service;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.PageResponse;
import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.dto.UpdateVotationSettingsRequest;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.dto.VotationFilterDTO;
import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.notification.VotationOptionsChangedNotificationType;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.IBadWeatherChecker;
import com.solnotfound.entity.weather.WeatherForecast;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ErrorCode;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.exception.InvalidVotationSettingsException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.exception.VotationVotesAtRiskException;
import com.solnotfound.exception.WeatherUnavailableException;
import com.solnotfound.listener.ActivityNotificationEvent;
import com.solnotfound.mapper.VotationMapper;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.IVotationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final IUserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public VotationService(
      IVotationRepository votationRepository,
      IActivityRepository activityRepository,
      IWeatherAdapter weatherAdapter,
      IBadWeatherChecker badWeatherChecker,
      IUserRepository userRepository,
      ApplicationEventPublisher eventPublisher) {
    this.votationRepository = votationRepository;
    this.activityRepository = activityRepository;
    this.weatherAdapter = weatherAdapter;
    this.badWeatherChecker = badWeatherChecker;
    this.userRepository = userRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Returns one page of the votations of activities the user organizes or joined, newest first.
   *
   * <p>Every filter is optional. {@code activityId} restricts the result to one activity, and
   * yields an empty page when the user does not belong to it. {@code votedByMe} keeps only
   * votations where the user already voted ({@code true}) or has not voted yet ({@code false}),
   * which lets clients list pending votes without loading every votation.
   *
   * @param userId authenticated user identifier
   * @param filter optional status, activity and vote filters
   * @param pageable requested page
   * @return one page of votations, each with the user's own vote in {@code votedOption}
   */
  public PageResponse<VotationDTO> search(
      String userId, VotationFilterDTO filter, Pageable pageable) {
    List<String> activityIds =
        Stream.concat(
                activityRepository.findActivitiesByOrganizerId(userId).stream(),
                activityRepository.findActivitiesByParticipantId(userId).stream())
            .map(Activity::getId)
            .distinct()
            .filter(id -> filter.activityId() == null || filter.activityId().equals(id))
            .toList();
    Page<VotationDTO> page =
        votationRepository
            .search(activityIds, filter, userId, pageable)
            .map(votation -> VotationMapper.toDTO(votation, userId));
    return PageResponse.from(page);
  }

  /**
   * Replaces the alternatives of an active votation, keeping the votes already cast on the dates
   * that survive the replacement.
   *
   * <p>The votation must stay coherent after the replacement, which adds two rules on top of the
   * per-date ones (duplicates, reprogramation range and weather):
   *
   * <ul>
   *   <li>every alternative must still be after the votation's closing date, so the votation never
   *       offers a date at or after its own deadline (the same rule {@link #updateVotationSettings}
   *       enforces when it moves the closing date);
   *   <li>an alternative that already carries votes can only be left out when the caller explicitly
   *       acknowledges the loss, otherwise those votes would disappear without anyone deciding so.
   * </ul>
   *
   * <p>Both rules are checked before the weather forecast is requested, so a rejected replacement
   * costs no provider call. A votation without a closing date cannot be compared against one and is
   * therefore left alone by the first rule.
   *
   * <p>The weather of a date is only requested for the dates this call introduces: the dates the
   * votation already publishes keep the forecast they were accepted with, so an edit that merely
   * reorders or drops dates performs no provider call at all, and the participants' existing votes
   * are never invalidated by a refreshed forecast.
   *
   * <p>When the resulting set of dates is not the one the votation had, the organizer and every
   * participant are notified ({@code VOTATION_OPTIONS_CHANGED}) after the new alternatives are
   * persisted, so nobody votes on a list they never saw. Saving the same dates again, in any order,
   * changes nothing and notifies nobody.
   *
   * @param votationId votation identifier
   * @param request the new alternatives and whether dropping voted ones is acknowledged
   * @param userId authenticated organizer identifier
   * @return the updated votation
   * @throws ResourceNotFoundException when the votation does not exist
   * @throws AccessDeniedException when the user is not the organizer
   * @throws InvalidVotationOptionsException when the votation is closed, a date is repeated, out of
   *     the activity's reprogramation range or a newly added date has bad weather
   * @throws InvalidVotationSettingsException when an alternative is not after the closing date
   * @throws VotationVotesAtRiskException when voted alternatives would be dropped without being
   *     acknowledged
   * @throws WeatherUnavailableException when the forecast range cannot be retrieved
   */
  public VotationDTO updateVotationOptions(
      String votationId, UpdateVotationOptionsRequest request, String userId) {
    Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      throw new ResourceNotFoundException(
          ErrorCode.VOTATION_NOT_FOUND, "Votation not found: " + votationId);
    }

    Activity activity = votation.getActivity();
    if (activity.getOrganizer() == null || !userId.equals(activity.getOrganizer().getId())) {
      throw new AccessDeniedException(
          ErrorCode.NOT_ORGANIZER, "Only the activity organizer can update votation options");
    }
    if (votation.getStatus() != com.solnotfound.entity.votation.VotationStatus.ACTIVE) {
      throw new InvalidVotationOptionsException(ErrorCode.VOTATION_CLOSED, request.dates());
    }
    if (new HashSet<>(request.dates()).size() != request.dates().size()) {
      throw new InvalidVotationOptionsException(request.dates());
    }

    LocalDateTime closingDate = votation.getClosingDate();
    if (closingDate != null) {
      Optional<LocalDateTime> notAfterClosing =
          request.dates().stream()
              .filter(date -> !date.isAfter(closingDate))
              .min(LocalDateTime::compareTo);
      if (notAfterClosing.isPresent()) {
        throw new InvalidVotationSettingsException(
            "Votation must close before its earliest alternative: " + notAfterClosing.get());
      }
    }

    List<VotationOption> droppedWithVotes =
        votation.getOptions().stream()
            .filter(existing -> !request.dates().contains(existing.getDateTime()))
            .filter(existing -> !existing.getUsers().isEmpty())
            .toList();
    if (!droppedWithVotes.isEmpty() && !Boolean.TRUE.equals(request.allowVoteLoss())) {
      throw new VotationVotesAtRiskException(
          droppedWithVotes.stream().map(VotationOption::getDateTime).toList(),
          droppedWithVotes.stream().mapToInt(option -> option.getUsers().size()).sum());
    }

    List<LocalDateTime> outOfRangeOptions =
        request.dates().stream()
            .filter(
                date ->
                    !activity.getReprogramationRange().isWithinRange(activity.getDateTime(), date))
            .toList();
    List<LocalDateTime> datesWithinRange =
        request.dates().stream().filter(date -> !outOfRangeOptions.contains(date)).toList();
    // Dates already published in this votation were validated against the forecast when they were
    // added, and participants may already have voted for them: re-checking them would spend a
    // provider call (and could reject an edit that only reorders or drops dates) to learn nothing
    // the votation does not already know. Only the dates this edit introduces are forecast.
    List<LocalDateTime> publishedDates =
        votation.getOptions().stream().map(VotationOption::getDateTime).toList();
    List<LocalDateTime> newDates =
        datesWithinRange.stream().filter(date -> !publishedDates.contains(date)).toList();
    List<WeatherForecast> forecasts =
        newDates.isEmpty()
            ? List.of()
            : weatherAdapter.getForecastRange(activity.getLocation(), newDates);
    if (forecasts.size() != newDates.size()) {
      throw new WeatherUnavailableException("Provider returned an incomplete forecast range");
    }
    List<LocalDateTime> invalidOptions = new ArrayList<>(outOfRangeOptions);
    for (int index = 0; index < forecasts.size(); index++) {
      if (badWeatherChecker.isBadWeatherForActivity(forecasts.get(index), activity)) {
        invalidOptions.add(newDates.get(index));
      }
    }
    if (!invalidOptions.isEmpty()) {
      throw new InvalidVotationOptionsException(invalidOptions);
    }

    List<VotationOption> existingOptions = votation.getOptions();
    Set<LocalDateTime> previousDates =
        existingOptions.stream().map(VotationOption::getDateTime).collect(Collectors.toSet());
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

    if (!previousDates.equals(new HashSet<>(request.dates()))) {
      eventPublisher.publishEvent(
          ActivityNotificationEvent.from(activity, new VotationOptionsChangedNotificationType()));
    }

    return VotationMapper.toDTO(votation, userId);
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
      throw new AccessDeniedException(
          ErrorCode.VOTATION_CLOSED, "Votation already closed: " + votationId);
    }
    Duration duration = request.duration();
    if (duration.isZero() || duration.isNegative()) {
      throw new InvalidVotationSettingsException("Duration must be greater than zero");
    }
    LocalDateTime closingDate = activity.now().plus(duration);
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
    return VotationMapper.toDTO(votation, userId);
  }

  private Votation findVotation(String votationId) {
    Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      throw new ResourceNotFoundException(
          ErrorCode.VOTATION_NOT_FOUND, "Votation not found: " + votationId);
    }
    return votation;
  }

  private Activity findActivity(Votation votation) {
    Activity activity = votation.getActivity();
    if (activity == null) {
      throw new ResourceNotFoundException(
          ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found for votation: " + votation.getId());
    }
    return activity;
  }

  private void requireOrganizer(Activity activity, String userId) {
    if (activity.getOrganizer() == null || !userId.equals(activity.getOrganizer().getId())) {
      throw new AccessDeniedException(
          ErrorCode.NOT_ORGANIZER, "Only the activity organizer can update the votation");
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
      throw new ResourceNotFoundException(
          ErrorCode.VOTATION_NOT_FOUND, "Votation not found: " + votationId);
    }
    if (!votation.isAnOption(vote)) {
      throw new ResourceNotFoundException(
          ErrorCode.VOTATION_OPTION_NOT_FOUND, "Option not found: " + vote);
    }
    final Activity activity = votation.getActivity();
    if (activity == null) {
      throw new ResourceNotFoundException(
          ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found for votation: " + votation.getId());
    }
    final User user =
        userRepository.save(
            activity
                .findOrganizerOrParticipant(userId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            ErrorCode.NOT_ACTIVITY_MEMBER,
                            "User doesn't belong to this activity: " + userId)));
    if (votation.getStatus() != VotationStatus.ACTIVE) {
      throw new AccessDeniedException(
          ErrorCode.VOTATION_CLOSED, "Votation already closed: " + votationId);
    }
    final Optional<LocalDateTime> currentVote = votation.getVoteByUser(user);
    if (currentVote.isPresent() && !currentVote.get().equals(vote)) {
      votation.unvote(currentVote.get(), user);
    }
    if (currentVote.isEmpty() || !currentVote.get().equals(vote)) {
      votation.vote(vote, user);
    }
    votationRepository.save(votation);
    return VotationMapper.toDTO(votation, userId);
  }
}
