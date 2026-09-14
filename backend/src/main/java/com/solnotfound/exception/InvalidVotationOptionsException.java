package com.solnotfound.exception;

import java.time.LocalDateTime;
import java.util.List;

public class InvalidVotationOptionsException extends RuntimeException {

  private final List<LocalDateTime> invalidOptionDates;

  public InvalidVotationOptionsException(List<LocalDateTime> invalidOptionDates) {
    super(
        "The following option dates do not meet the activity's weather requirements: "
            + invalidOptionDates);
    this.invalidOptionDates = List.copyOf(invalidOptionDates);
  }

  public List<LocalDateTime> getInvalidOptionDates() {
    return invalidOptionDates;
  }
}
