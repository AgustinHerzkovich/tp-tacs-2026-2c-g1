package com.solnotfound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.IBadWeatherChecker;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.repository.VotationMockRepository;
import com.solnotfound.service.VotationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class VotationControllerTest {

  @Test
  void returnsVotationsForActivitiesWhereUserIsOrganizerOrParticipant() {
    VotationMockRepository repository = new VotationMockRepository();
    repository.save(votation(activity(user("1"), List.of(user("2")))));
    repository.save(votation(activity(user("3"), List.of(user("1")))));
    repository.save(votation(activity(user("4"), List.of(user("5")))));
    VotationController controller =
        new VotationController(
            new VotationService(
                repository, mock(IWeatherAdapter.class), mock(IBadWeatherChecker.class)));

    ResponseEntity<List<VotationDTO>> response = controller.getByOrganizerOrParticipantId("1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  private Votation votation(Activity activity) {
    Votation votation = new Votation();
    votation.setActivity(activity);
    votation.setCreationDate(LocalDateTime.of(2026, 8, 28, 20, 0));
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setOptions(List.of());
    return votation;
  }

  private Activity activity(User organizer, List<User> participants) {
    Activity activity = new Activity();
    activity.setOrganizer(organizer);
    activity.setParticipants(participants);
    return activity;
  }

  private User user(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
