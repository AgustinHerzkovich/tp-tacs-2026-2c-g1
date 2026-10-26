package com.solnotfound.dto;

import java.time.Instant;

public record ProviderStatusPoint(Instant at, String status, String circuitBreakerState) {}
