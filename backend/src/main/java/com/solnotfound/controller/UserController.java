package com.solnotfound.controller;

import com.solnotfound.dto.LinkTelegramChatRequest;
import com.solnotfound.dto.TelegramLinkCodeResponse;
import com.solnotfound.dto.UserDTO;
import com.solnotfound.service.UserNameRecorder;
import com.solnotfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/telegram/{chatId}")
  public ResponseEntity<UserDTO> getUserByTelegramChatId(@PathVariable Long chatId) {
    return ResponseEntity.ok(userService.getUserByTelegramChatId(chatId));
  }

  @PostMapping("/telegram/{chatId}/link-code")
  public ResponseEntity<TelegramLinkCodeResponse> createTelegramLinkCode(
      @PathVariable Long chatId) {
    return ResponseEntity.ok(userService.createTelegramLinkCode(chatId));
  }

  @PutMapping("/me/telegram")
  public ResponseEntity<UserDTO> linkTelegramChat(
      @Valid @RequestBody LinkTelegramChatRequest request, Authentication authentication) {
    Jwt jwt = jwt(authentication);
    return ResponseEntity.ok(
        userService.linkTelegramChat(
            jwt.getSubject(), UserNameRecorder.displayName(jwt), request.code()));
  }

  private Jwt jwt(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) return jwt;
    throw new IllegalStateException("Authenticated principal must be a JWT");
  }
}
