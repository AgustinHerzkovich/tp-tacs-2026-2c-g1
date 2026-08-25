package com.solnotfound.dto;

import com.solnotfound.entity.ActivityType;
import java.time.LocalDateTime;

public record ActivityFilterDTO(
    ActivityType type,
    String city,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    Boolean availability) {}
