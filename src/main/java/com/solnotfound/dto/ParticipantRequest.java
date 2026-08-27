package com.solnotfound.dto;

import jakarta.validation.constraints.NotBlank;

public record ParticipantRequest(@NotBlank String userId) {}
