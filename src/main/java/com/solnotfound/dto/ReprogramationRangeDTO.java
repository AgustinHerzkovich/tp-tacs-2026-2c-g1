package com.solnotfound.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public record ReprogramationRangeDTO(
    @NotNull @Positive Integer maxDays,
    @NotNull LocalTime initialHour,
    @NotNull LocalTime finalHour) {}
