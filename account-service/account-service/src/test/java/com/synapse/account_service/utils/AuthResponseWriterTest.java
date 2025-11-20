package com.synapse.account_service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.account_service.config.CookieProperties;
import com.synapse.account_service_api.dto.TokenResult;
import com.synapse.account_service_api.dto.response.RefreshTokenResponse;
import com.synapse.account_service_api.dto.response.TokenResponse;

class AuthResponseWriterTest {

    private AuthResponseWriter authResponseWriter;
    private CookieProperties cookieProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cookieProperties = mock(CookieProperties.class);
        objectMapper = new ObjectMapper();
        authResponseWriter = new AuthResponseWriter(objectMapper, cookieProperties);
    }

    @Test
    void writeSuccessResponse_ShouldUseCookieProperties() {
        // Given
        when(cookieProperties.isSecure()).thenReturn(true);
        when(cookieProperties.getSameSite()).thenReturn("None");
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.getPath()).thenReturn("/");
        when(cookieProperties.getDomain()).thenReturn("example.com");

        TokenResponse tokenResponse = new TokenResponse(
                new TokenResult("access-token", Instant.now().plusSeconds(3600)),
                new TokenResult("refresh-token", Instant.now().plusSeconds(7200)));

        // When
        RefreshTokenResponse response = authResponseWriter.writeSuccessResponse(tokenResponse);
        ResponseCookie cookie = response.cookie();

        // Then
        assertEquals("refreshToken", cookie.getName());
        assertEquals("refresh-token", cookie.getValue());
        assertTrue(cookie.isSecure());
        assertEquals("None", cookie.getSameSite());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("example.com", cookie.getDomain());
    }
}
