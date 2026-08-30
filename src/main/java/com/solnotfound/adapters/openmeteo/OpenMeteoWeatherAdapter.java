package com.solnotfound.adapters.openmeteo;

import com.solnotfound.adapters.IWeatherAdapter;
import com.solnotfound.entity.Location;
import com.solnotfound.entity.WeatherForecast;
import com.solnotfound.exception.WeatherUnavailableException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "weather.provider", havingValue = "open-meteo", matchIfMissing = true)
@EnableConfigurationProperties(OpenMeteoProperties.class)
public class OpenMeteoWeatherAdapter implements IWeatherAdapter {

  private final OpenMeteoClient client;

  public OpenMeteoWeatherAdapter(OpenMeteoClient client) {
    this.client = client;
  }

  @Override
  public WeatherForecast getWeather(Location location) {
    Coordinates coordinates = resolveCoordinates(location);
    OpenMeteoDtos.ForecastResponse response =
        client.getCurrent(coordinates.latitude(), coordinates.longitude());
    if (response == null || response.current() == null) {
      throw new WeatherUnavailableException("Current weather is unavailable");
    }
    OpenMeteoDtos.Current current = response.current();
    return forecast(
        current.time(), current.temperature(), current.rainProbability(), current.windSpeed());
  }

  @Override
  public WeatherForecast getFutureClimate(Location location, LocalDateTime dateTime) {
    return getForecastRange(location, List.of(dateTime)).getFirst();
  }

  /**
   * Fetches one hourly forecast window and maps each requested local time to its nearest hour. Ties
   * are resolved toward the future hour. Dates beyond Open-Meteo's 16-day horizon are rejected.
   *
   * @param location activity location
   * @param dateTimes requested activity or alternative date-times
   * @return forecasts ordered like {@code dateTimes}
   * @throws WeatherUnavailableException when coordinates, horizon, or provider data are unavailable
   */
  @Override
  public List<WeatherForecast> getForecastRange(Location location, List<LocalDateTime> dateTimes) {
    if (dateTimes.isEmpty()) {
      return List.of();
    }
    LocalDateTime start = dateTimes.stream().min(LocalDateTime::compareTo).orElseThrow();
    LocalDateTime end = dateTimes.stream().max(LocalDateTime::compareTo).orElseThrow();
    return getForecastWindow(location, start.toLocalDate(), end.toLocalDate()).stream()
        .collect(
            Collectors.collectingAndThen(
                Collectors.toMap(
                    WeatherForecast::getDateTime, value -> value, (first, second) -> first),
                byTime -> dateTimes.stream().map(time -> nearest(byTime, time)).toList()));
  }

  private List<WeatherForecast> getForecastWindow(
      Location location, LocalDate startDate, LocalDate endDate) {
    validateHorizon(startDate, endDate);
    Coordinates coordinates = resolveCoordinates(location);
    return mapHourly(
        client.getForecast(coordinates.latitude(), coordinates.longitude(), startDate, endDate));
  }

  private List<WeatherForecast> mapHourly(OpenMeteoDtos.ForecastResponse response) {
    if (response == null || response.hourly() == null) {
      throw new WeatherUnavailableException("Forecast data is unavailable");
    }
    OpenMeteoDtos.Hourly hourly = response.hourly();
    int size = hourly.time() == null ? 0 : hourly.time().size();
    if (size == 0
        || hourly.temperatures() == null
        || hourly.rainProbabilities() == null
        || hourly.windSpeeds() == null
        || hourly.temperatures().size() != size
        || hourly.rainProbabilities().size() != size
        || hourly.windSpeeds().size() != size) {
      throw new WeatherUnavailableException("Forecast response is incomplete");
    }
    List<WeatherForecast> forecasts = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      forecasts.add(
          forecast(
              hourly.time().get(index),
              hourly.temperatures().get(index),
              hourly.rainProbabilities().get(index),
              hourly.windSpeeds().get(index)));
    }
    return List.copyOf(forecasts);
  }

  private WeatherForecast nearest(
      Map<LocalDateTime, WeatherForecast> byTime, LocalDateTime requestedTime) {
    LocalDateTime rounded = requestedTime.truncatedTo(ChronoUnit.HOURS);
    if (requestedTime.getMinute() >= 30) {
      rounded = rounded.plusHours(1);
    }
    WeatherForecast result = byTime.get(rounded);
    if (result == null) {
      throw new WeatherUnavailableException("Forecast is unavailable for " + requestedTime);
    }
    return new WeatherForecast(
        requestedTime, result.getTemperature(), result.getChanceOfRain(), result.getWindSpeed());
  }

  private WeatherForecast forecast(
      LocalDateTime time, Float temperature, Float rainProbability, Float windSpeed) {
    if (time == null || temperature == null || rainProbability == null || windSpeed == null) {
      throw new WeatherUnavailableException("Weather response is incomplete");
    }
    return new WeatherForecast(time, temperature, rainProbability, windSpeed);
  }

  private void validateHorizon(LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.now();
    if (startDate.isBefore(today) || endDate.isAfter(today.plusDays(15))) {
      throw new WeatherUnavailableException("Forecast is outside the 16-day provider horizon");
    }
  }

  private Coordinates resolveCoordinates(Location location) {
    if (location.latitude() != null && location.longitude() != null) {
      return new Coordinates(location.latitude(), location.longitude());
    }
    if (location.city() == null
        || location.city().name() == null
        || location.city().name().isBlank()) {
      throw new WeatherUnavailableException("Weather location has no coordinates or city");
    }
    OpenMeteoDtos.GeocodingResponse response = client.geocode(location.city().name());
    if (response == null || response.results() == null || response.results().isEmpty()) {
      throw new WeatherUnavailableException(
          "No coordinates were found for city " + location.city().name());
    }
    OpenMeteoDtos.GeocodingResult result = response.results().getFirst();
    return new Coordinates(result.latitude(), result.longitude());
  }

  public record Coordinates(Double latitude, Double longitude) {}
}
