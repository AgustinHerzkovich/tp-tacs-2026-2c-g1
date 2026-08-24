package com.solnotfound.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.ActivityType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CreateActivityRequestValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsBlankTitleAndInvalidCoordinates() {
    LocationDTO location = new LocationDTO(null, 91.0, -181.0);
    CreateActivityRequest request =
        new CreateActivityRequest(
            " ",
            null,
            ActivityType.OUTDOOR,
            location,
            LocalDateTime.now().plusDays(1),
            1,
            10,
            new WeatherConditionsDTO(30, 10, 28, 25.0),
            15);

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("title", "location.latitude", "location.longitude");
  }
}
