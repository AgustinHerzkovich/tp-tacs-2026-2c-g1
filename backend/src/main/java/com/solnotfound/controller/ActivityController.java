package com.solnotfound.controller;

import com.solnotfound.dto.ActivityFilterDTO;
import com.solnotfound.dto.ActivityResponse;
import com.solnotfound.dto.ActivityWeatherResponse;
import com.solnotfound.dto.CreateActivityRequest;
import com.solnotfound.dto.PageResponse;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.exception.InvalidPageRequestException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
   * @param timeZoneId caller's IANA time zone (e.g. "America/Argentina/Buenos_Aires"), used to
   *     judge whether {@code request.dateTime()} is in the future; falls back to the server's own
   *     zone when absent
   * @param jwt verified access token of the authenticated user
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
      @RequestHeader(value = "X-Time-Zone", required = false) String timeZoneId,
      Authentication authentication) {
    List<MultipartImageFile> imageFiles =
        Objects.requireNonNullElse(images, List.<MultipartFile>of()).stream()
            .map(MultipartImageFile::new)
            .toList();
    ActivityResponse createdActivity =
        activityService.create(request, jwt(authentication).getSubject(), imageFiles, timeZoneId);
    return ResponseEntity.created(URI.create("/activities/" + createdActivity.id()))
        .body(createdActivity);
  }

  private record CreateActivityMultipartRequest(
      CreateActivityRequest activity, List<MultipartFile> images) {}

  @GetMapping
  public ResponseEntity<PageResponse<ActivityResponse>> getAll(
      @RequestParam(required = false) ActivityType type,
      @RequestParam(required = false) List<ActivityStatus> status,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) List<String> ids,
      @RequestParam(required = false) Boolean availability,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    PageResponse<ActivityResponse> activities =
        activityService.search(
            new ActivityFilterDTO(
                type,
                city,
                dateFrom,
                dateTo,
                availability,
                status == null ? List.of() : status,
                title,
                ids),
            pageRequest(page, size, "dateTime"));
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
    return ResponseEntity.ok(activityService.join(id, jwt(authentication).getSubject()));
  }

  @DeleteMapping("/{id}/participants/me")
  public ResponseEntity<ActivityResponse> leave(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(activityService.leave(id, jwt(authentication).getSubject()));
  }

  @GetMapping("/{id}/weather")
  public ResponseEntity<ActivityWeatherResponse> getWeather(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(activityService.getWeather(id, jwt(authentication).getSubject()));
  }

  @GetMapping("/organizers/me")
  public ResponseEntity<PageResponse<ActivityResponse>> getOrganized(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return ResponseEntity.ok(
        activityService.getByOrganizerId(
            jwt(authentication).getSubject(), pageRequest(page, size, "dateTime")));
  }

  @GetMapping("/participants/me")
  public ResponseEntity<PageResponse<ActivityResponse>> getJoined(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return ResponseEntity.ok(
        activityService.getByParticipantId(
            jwt(authentication).getSubject(), pageRequest(page, size, "dateTime")));
  }

  private PageRequest pageRequest(int page, int size, String sortProperty) {
    if (page < 0 || size < 1 || size > 100) {
      throw new InvalidPageRequestException(
          "Page must be non-negative and size must be between 1 and 100");
    }
    return PageRequest.of(page, size, Sort.by(sortProperty).ascending());
  }

  private Jwt jwt(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) return jwt;
    throw new IllegalStateException("Authenticated principal must be a JWT");
  }
}
