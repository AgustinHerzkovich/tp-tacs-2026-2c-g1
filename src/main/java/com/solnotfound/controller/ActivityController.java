package com.solnotfound.controller;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.ActivityWeatherResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.entity.ActivityType;
import com.solnotfound.service.ActivityService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
public class ActivityController {
  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @PostMapping
  public ResponseEntity<ActivityResponse> create(
      @Valid @RequestBody CreateActivityRequest request, Authentication authentication) {
    ActivityResponse createdActivity =
        activityService.create(request, currentUserId(authentication));
    return ResponseEntity.created(URI.create("/activities/" + createdActivity.id()))
        .body(createdActivity);
  }

  @GetMapping
  public ResponseEntity<List<ActivityResponse>> getAll(
      @RequestParam(required = false) ActivityType type,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) Boolean availability,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateTo) {
    List<ActivityResponse> activities =
        activityService.search(new ActivityFilterDTO(type, city, dateFrom, dateTo, availability));
    return ResponseEntity.ok(activities);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ActivityResponse> getById(@PathVariable String id) {
    ActivityResponse activity = activityService.getById(id);
    if (activity == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(activity);
  }

  @PutMapping("/{id}/participants/me")
  public ResponseEntity<ActivityResponse> join(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(activityService.join(id, currentUserId(authentication)));
  }

  @DeleteMapping("/{id}/participants/me")
  public ResponseEntity<ActivityResponse> leave(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(activityService.leave(id, currentUserId(authentication)));
  }

  @GetMapping("/{id}/weather")
  public ResponseEntity<ActivityWeatherResponse> getWeather(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(activityService.getWeather(id, currentUserId(authentication)));
  }

  private String currentUserId(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return "development-user";
  }

  @GetMapping("/organizer/{id}")
  public ResponseEntity<List<ActivityResponse>> getByOrganizerId(@PathVariable String id) {
    List<ActivityResponse> activities = activityService.getByOrganizerId(id);
    if (activities == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(activities);
  }

  @GetMapping("/participant/{id}")
  public ResponseEntity<List<ActivityResponse>> getByParticipantId(@PathVariable String id) {
    List<ActivityResponse> activities = activityService.getByParticipantId(id);
    if (activities == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(activities);
  }
}
