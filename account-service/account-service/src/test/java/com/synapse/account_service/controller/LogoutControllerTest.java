package com.synapse.account_service.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.synapse.account_service.TestConfig;
import com.synapse.account_service.service.TokenManagementService;

import jakarta.servlet.http.Cookie;

public class LogoutControllerTest extends TestConfig {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenManagementService tokenManagementService;

    @Test
    @DisplayName("로그아웃 성공 - 쿠키가 있는 경우")
    void logout_success_withCookie() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        doNothing().when(tokenManagementService).logout(anyString());

        // when & then
        mockMvc.perform(post("/api/accounts/logout")
                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0)) // 쿠키 삭제 확인
                .andExpect(cookie().path("refreshToken", "/"));

        verify(tokenManagementService).logout(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 성공 - 쿠키가 없는 경우에도 성공 처리 (쿠키 삭제만 수행)")
    void logout_success_withoutCookie() throws Exception {
        // when & then
        mockMvc.perform(post("/api/accounts/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0)); // 쿠키 삭제 확인
    }
}
