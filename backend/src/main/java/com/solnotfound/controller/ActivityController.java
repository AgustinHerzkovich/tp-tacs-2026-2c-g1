package com.solnotfound.controller;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.ActivityWeatherResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.service.ActivityService;
import com.solnotfound.storage.MultipartImageFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/activities")
public class ActivityController {
  private final ActivityService activityService;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared activity service")
  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  /**
   * Creates an activity and stores its optional images in the configured object storage.
   *
   * @param request JSON activity data from the multipart {@code activity} part
   * @param images optional image parts
   * @param authentication current authenticated user
   * @return the created activity with temporary image URLs
   */
  @Operation(
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = CreateActivityMultipartRequest.class),
                      encoding =
                          @Encoding(
                              name = "activity",
                              contentType = MediaType.APPLICATION_JSON_VALUE))))
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ActivityResponse> create(
      @Valid @RequestPart("activity") CreateActivityRequest request,
      @RequestPart(value = "images", required = false) List<MultipartFile> images,
      Authentication authentication) {
    List<MultipartImageFile> imageFiles =
        Objects.requireNonNullElse(images, List.<MultipartFile>of()).stream()
            .map(MultipartImageFile::new)
            .toList();
    ActivityResponse createdActivity =
        activityService.create(request, currentUserId(authentication), imageFiles);
    return ResponseEntity.created(URI.create("/activities/" + createdActivity.id()))
        .body(createdActivity);
  }

  private record CreateActivityMultipartRequest(
      CreateActivityRequest activity, List<MultipartFile> images) {}

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

  @GetMapping("/organizers/me")
  public ResponseEntity<List<ActivityResponse>> getOrganized(Authentication authentication) {
    return ResponseEntity.ok(activityService.getByOrganizerId(currentUserId(authentication)));
  }

  @GetMapping("/participants/me")
  public ResponseEntity<List<ActivityResponse>> getJoined(Authentication authentication) {
    return ResponseEntity.ok(activityService.getByParticipantId(currentUserId(authentication)));
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
}
