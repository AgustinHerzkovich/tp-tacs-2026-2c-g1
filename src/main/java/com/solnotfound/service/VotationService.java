package com.solnotfound.service;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationOption;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.exception.InvalidVotationOptionsException;
import com.solnotfound.mapper.VotationMapper;
import com.solnotfound.mapper.VotationOptionMapper;
import com.solnotfound.repository.VotationRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VotationService {

  private final VotationRepository votationRepository;
  private final IWeatherAdapter weatherAdapter;
  private final IBadWeatherChecker badWeatherChecker;

  public VotationService(
      VotationRepository votationRepository,
      IWeatherAdapter weatherAdapter,
      IBadWeatherChecker badWeatherChecker) {
    this.votationRepository = votationRepository;
    this.weatherAdapter = weatherAdapter;
    this.badWeatherChecker = badWeatherChecker;
  }

  public List<VotationDTO> getByOrganizerOrParticipantId(String userId) {
    List<Votation> votations = votationRepository.findByOrganizerOrParticipantId(userId);
    if (votations == null) {
      return null;
    }
    return votations.stream().map(VotationMapper::toDTO).toList();
  }

  public List<VotationDTO> updateVotationOptions(
      String votationId, @Valid List<VotationOptionDTO> request) {
    Votation votation = votationRepository.findById(votationId);
    if (votation == null) {
      return null;
    }

    Activity activity = votation.getActivity();
    List<VotationOptionDTO> invalidOptions =
        request.stream().filter(optionDTO -> isBadWeatherAt(activity, optionDTO)).toList();
    if (!invalidOptions.isEmpty()) {
      throw new InvalidVotationOptionsException(
          invalidOptions.stream().map(VotationOptionDTO::dateTime).toList());
    }

    List<VotationOption> newOptions = request.stream().map(VotationOptionMapper::toEntity).toList();
    votation.setOptions(newOptions);
    votationRepository.update(votation);

    return List.of(VotationMapper.toDTO(votation));
  }

  private boolean isBadWeatherAt(Activity activity, VotationOptionDTO optionDTO) {
    WeatherForecast weather =
        weatherAdapter.getFutureClimate(activity.getLocation(), optionDTO.dateTime());
    return badWeatherChecker.isBadWeatherForActivity(weather, activity);
  }
}
