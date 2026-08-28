package com.solnotfound.controller;


import com.solnotfound.exception.AccessDeniedException;
import com.solnotfound.exception.ResourceNotFoundException;
import com.solnotfound.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  @Test
  void getNotifications_ReturnsOk() throws Exception {
    mockMvc.perform(get("/notifications/me")).andExpect(status().isOk());
  }

  @Test
  void markAsRead_ReturnsOk() throws Exception {
    doNothing().when(notificationService).markAsRead("notif-1", "hardcodeado123");

    mockMvc.perform(patch("/notifications/notif-1")).andExpect(status().isOk());
  }

  @Test
  void markAsRead_ReturnsNotFound_WhenNotificationDoesNotExist() throws Exception {
    doThrow(new ResourceNotFoundException("No se encontró la notificación")).when(notificationService).markAsRead("notif-inexistente", "hardcodeado123");

    mockMvc.perform(patch("/notifications/notif-inexistente")).andExpect(status().isNotFound());
  }

  @Test
  void markAsRead_ReturnsForbidden_WhenUserIsNotOwner() throws Exception {
    doThrow(new AccessDeniedException("No tienes permisos")).when(notificationService).markAsRead("notif-1", "hardcodeado123");

    mockMvc.perform(patch("/notifications/notif-1")).andExpect(status().isForbidden());
  }
}
