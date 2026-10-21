package com.solnotfound.dto;

import com.solnotfound.entity.user.WeatherProvider;
import jakarta.validation.constraints.NotNull;

public record UserPreferencesDTO(@NotNull WeatherProvider weatherProvider) {}
