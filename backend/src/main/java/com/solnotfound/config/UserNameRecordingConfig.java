package com.solnotfound.config;

import com.solnotfound.service.UserNameRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Records the display name of the authenticated user on every request of the user-facing API. The
 * Telegram bot and the scheduler endpoints are left out: their callers are machines, not users.
 */
@Slf4j
@Configuration
public class UserNameRecordingConfig implements WebMvcConfigurer {

  private final UserNameRecorder userNameRecorder;

  public UserNameRecordingConfig(UserNameRecorder userNameRecorder) {
    this.userNameRecorder = userNameRecorder;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new RecordingInterceptor())
        .excludePathPatterns("/users/telegram/**", "/internal/**", "/healthcheck");
  }

  private final class RecordingInterceptor implements HandlerInterceptor {

    /**
     * Records the name before the request is handled, so a response built in the same request
     * already shows it. A failure is logged and ignored: the name is cosmetic and must never make
     * an otherwise valid request fail.
     */
    @Override
    public boolean preHandle(
        HttpServletRequest request, HttpServletResponse response, Object handler) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        try {
          userNameRecorder.record(jwt);
        } catch (RuntimeException exception) {
          log.warn("Could not record the user name: userId={}", jwt.getSubject(), exception);
        }
      }
      return true;
    }
  }
}
