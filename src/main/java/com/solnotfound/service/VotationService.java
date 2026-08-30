package com.solnotfound.service;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.UpdateVotationOptionsRequest;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationOption;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.mapper.VotationMapper;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IVotationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        java.util.stream.Stream.concat(
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

    List<LocalDateTime> invalidOptions =
        request.dates().stream()
            .filter(
                date ->
                    !activity.getReprogramationRange().isWithinRange(activity.getDateTime(), date))
            .toList();
    invalidOptions =
        java.util.stream.Stream.concat(
                invalidOptions.stream(),
                request.dates().stream().filter(date -> isBadWeatherAt(activity, date)))
            .distinct()
            .toList();
    if (!invalidOptions.isEmpty()) {
      throw new InvalidVotationOptionsException(invalidOptions);
    }

    List<VotationOption> newOptions = new ArrayList<>();
    for (LocalDateTime date : request.dates()) {
      VotationOption option = new VotationOption();
      option.setDateTime(date);
      option.setUsers(List.of());
      newOptions.add(option);
    }
    votation.setOptions(newOptions);
    votationRepository.save(votation);

    return VotationMapper.toDTO(votation);
  }

  private boolean isBadWeatherAt(Activity activity, LocalDateTime dateTime) {
    WeatherForecast weather = weatherAdapter.getFutureClimate(activity.getLocation(), dateTime);
    return badWeatherChecker.isBadWeatherForActivity(weather, activity);
  }
}
