package com.solnotfound.controller;

import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.WeatherForecastStatisticsResponse;
import com.solnotfound.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {
  private final StatisticsService statisticsService;
  public StatisticsController(StatisticsService statisticsService) {this.statisticsService = statisticsService;}
  @GetMapping("/weatherForecast")
  public ResponseEntity<WeatherForecastStatisticsResponse> getWeatherForecastServiceStatistics(
    @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> endDate) {

    LocalDate from = startDate.orElse(LocalDate.now().minusDays(7));
    LocalDate to = endDate.orElse(LocalDate.now());

    WeatherForecastStatisticsResponse weatherForecastStatisticsResponse = statisticsService.getWeatherForecastServiceStatistics(from, to);
    return ResponseEntity.ok(weatherForecastStatisticsResponse);
  }
}
