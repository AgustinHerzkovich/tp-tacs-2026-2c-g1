package com.solnotfound.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.VotationDTO;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VotationMapperTest {

  @Test
  void mapsVotationToDTO() {
    LocalDateTime creationDate = LocalDateTime.of(2026, 8, 28, 19, 30);
    LocalDateTime optionDate = LocalDateTime.of(2026, 8, 29, 10, 0);
    Votation votation = new Votation();
    votation.setId("1");
    com.solnotfound.entity.activity.Activity activity =
        new com.solnotfound.entity.activity.Activity();
    activity.setId("activity-1");
    votation.setActivity(activity);
    assertThat(votation.getActivity()).isSameAs(activity);
    votation.setCreationDate(creationDate);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setOptions(List.of(option(optionDate, user("2", "Jane Doe", "jane@example.com"))));

    VotationDTO dto = VotationMapper.toDTO(votation);

    assertThat(dto.id()).isEqualTo("1");
    assertThat(dto.activityId()).isEqualTo("activity-1");
    assertThat(dto.creationDate()).isEqualTo(creationDate);
    assertThat(dto.status()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(dto.options()).hasSize(1);
    assertThat(dto.options().getFirst().dateTime()).isEqualTo(optionDate);
    assertThat(dto.options().getFirst().voteCount()).isEqualTo(1);
    assertThat(dto.options().getFirst().voterNames()).containsExactly("Jane Doe");
  }

  @Test
  void mapsNullValues() {
    assertThat(VotationMapper.toDTO(null)).isNull();
  }

  private VotationOption option(LocalDateTime dateTime, User user) {
    VotationOption option = new VotationOption();
    option.setDateTime(dateTime);
    option.setUsers(List.of(user));
    return option;
  }

  private User user(String id, String name, String email) {
    User user = new User();
    user.setId(id);
    user.setName(name);
    user.setEmail(email);
    return user;
  }
}
