package com.solnotfound.adapters.openmeteo;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.open-meteo")
public record OpenMeteoProperties(URI forecastUrl, URI geocodingUrl) {}
