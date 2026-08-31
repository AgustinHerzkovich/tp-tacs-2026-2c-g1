package com.solnotfound.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

public record UpdateVotationSettingsRequest(
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double minQuorum, @NotNull Duration duration) {}
