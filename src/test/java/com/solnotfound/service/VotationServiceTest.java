package com.solnotfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationOption;
import com.solnotfound.entity.VotationStatus;
import com.solnotfound.repository.VotationMockRepository;
import com.solnotfound.repository.VotationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VotationServiceTest {

  private VotationService votationService;
  private VotationMockRepository votationRepository;

  @BeforeEach
  void setUp() {
    votationRepository = new VotationMockRepository();
    votationService = new VotationService(votationRepository);
  }

  @Test
  void returnsVotationsWhereUserIsOrganizerOrParticipant() {
    LocalDateTime creationDate = LocalDateTime.of(2026, 8, 28, 20, 0);
    votationRepository.save(votation("v-1", activity(user("1"), List.of(user("2"))), creationDate));
    votationRepository.save(votation("v-2", activity(user("3"), List.of(user("1"))), creationDate));
    votationRepository.save(votation("v-3", activity(user("4"), List.of(user("5"))), creationDate));

    List<VotationDTO> results = votationService.getByOrganizerOrParticipantId("1");

    assertThat(results).extracting(VotationDTO::id).containsExactly("v-1", "v-2");
  }

  @Test
  void mapsVotationFieldsAndOptions() {
    LocalDateTime creationDate = LocalDateTime.of(2026, 8, 28, 20, 0);
    LocalDateTime optionDate = LocalDateTime.of(2026, 8, 29, 10, 0);
    Votation votation = votation("v-1", activity(user("1"), List.of(user("2"))), creationDate);
    votation.setOptions(List.of(option(optionDate, List.of(user("2")))));
    votationRepository.save(votation);

    VotationDTO dto = votationService.getByOrganizerOrParticipantId("1").getFirst();

    assertThat(dto.id()).isEqualTo("v-1");
    assertThat(dto.creationDate()).isEqualTo(creationDate);
    assertThat(dto.status()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(dto.activity().organizer().id()).isEqualTo("1");
    assertThat(dto.activity().participants()).extracting(id -> id.id()).containsExactly("2");
    assertThat(dto.options().getFirst().dateTime()).isEqualTo(optionDate);
    assertThat(dto.options().getFirst().users().getFirst().id()).isEqualTo("2");
  }

  @Test
  void returnsEmptyListWhenUserOrganizesOrParticipatesInNoVotation() {
    votationRepository.save(votation("v-1", activity(user("2"), List.of(user("3"))), null));

    assertThat(votationService.getByOrganizerOrParticipantId("1")).isEmpty();
  }

  @Test
  void returnsEmptyListWhenNoVotationsExist() {
    assertThat(votationService.getByOrganizerOrParticipantId("1")).isEmpty();
  }

  @Test
  void ignoresVotationsWithoutActivity() {
    LocalDateTime creationDate = LocalDateTime.of(2026, 8, 28, 20, 0);
    votationRepository.save(votation("v-1", null, creationDate));
    votationRepository.save(votation("v-2", activity(user("1"), List.of()), creationDate));

    List<VotationDTO> results = votationService.getByOrganizerOrParticipantId("1");

    assertThat(results).extracting(VotationDTO::id).containsExactly("v-2");
  }

  @Test
  void returnsNullWhenRepositoryReturnsNull() {
    VotationRepository repository = mock(VotationRepository.class);
    when(repository.findByOrganizerOrParticipantId("1")).thenReturn(null);
    VotationService service = new VotationService(repository);

    assertThat(service.getByOrganizerOrParticipantId("1")).isNull();
  }

  private Votation votation(String id, Activity activity, LocalDateTime creationDate) {
    Votation votation = new Votation();
    votation.setId(id);
    votation.setActivity(activity);
    votation.setCreationDate(creationDate);
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

  private VotationOption option(LocalDateTime dateTime, List<User> users) {
    VotationOption option = new VotationOption();
    option.setDateTime(dateTime);
    option.setUsers(users);
    return option;
  }

  private User user(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
