package com.solnotfound.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.dto.UserDTO;
import com.solnotfound.dto.VotationDTO;
import com.solnotfound.dto.VotationOptionDTO;
import com.solnotfound.entity.Activity;
import com.solnotfound.entity.User;
import com.solnotfound.entity.Votation;
import com.solnotfound.entity.VotationOption;
import com.solnotfound.entity.VotationStatus;
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
    votation.setActivity(new Activity());
    votation.setCreationDate(creationDate);
    votation.setStatus(VotationStatus.ACTIVE);
    votation.setOptions(List.of(option(optionDate, user("2", "Jane Doe", "jane@example.com"))));

    VotationDTO dto = VotationMapper.toDTO(votation);

    assertThat(dto.id()).isEqualTo("1");
    assertThat(dto.activity()).isNotNull();
    assertThat(dto.creationDate()).isEqualTo(creationDate);
    assertThat(dto.status()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(dto.options()).hasSize(1);
    assertThat(dto.options().getFirst().dateTime()).isEqualTo(optionDate);
    assertThat(dto.options().getFirst().users()).hasSize(1);
    assertThat(dto.options().getFirst().users().getFirst().id()).isEqualTo("2");
  }

  @Test
  void mapsDTOToVotation() {
    LocalDateTime creationDate = LocalDateTime.of(2026, 8, 28, 19, 30);
    LocalDateTime optionDate = LocalDateTime.of(2026, 8, 29, 10, 0);
    VotationDTO dto =
        new VotationDTO(
            "1",
            ActivityMapper.toDTO(new Activity()),
            creationDate,
            VotationStatus.ACTIVE,
            List.of(
                new VotationOptionDTO(
                    optionDate, List.of(new UserDTO("2", "Jane Doe", "jane@example.com")))));

    Votation votation = VotationMapper.toEntity(dto);

    assertThat(votation.getId()).isEqualTo("1");
    assertThat(votation.getActivity()).isNotNull();
    assertThat(votation.getCreationDate()).isEqualTo(creationDate);
    assertThat(votation.getStatus()).isEqualTo(VotationStatus.ACTIVE);
    assertThat(votation.getOptions()).hasSize(1);
    assertThat(votation.getOptions().getFirst().getDateTime()).isEqualTo(optionDate);
    assertThat(votation.getOptions().getFirst().getUsers()).hasSize(1);
    assertThat(votation.getOptions().getFirst().getUsers().getFirst().getId()).isEqualTo("2");
  }

  @Test
  void mapsNullValues() {
    assertThat(VotationMapper.toDTO(null)).isNull();
    assertThat(VotationMapper.toEntity(null)).isNull();
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
