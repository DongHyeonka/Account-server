package com.synapse.account_service.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.account_service.config.CookieProperties;
import com.synapse.account_service_api.dto.response.AccessTokenResponse;
import com.synapse.account_service_api.dto.response.RefreshTokenResponse;
import com.synapse.account_service_api.dto.response.TokenResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthResponseWriter {
    private final ObjectMapper objectMapper;
    private final CookieProperties cookieProperties;

    public RefreshTokenResponse writeSuccessResponse(TokenResponse tokenResponse) {
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.from(tokenResponse.accessToken());
        long maxAge = Duration.between(Instant.now(), tokenResponse.refreshToken().expiresAt()).getSeconds();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponse.refreshToken().token())
                .maxAge(maxAge)
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .build();

        return new RefreshTokenResponse(cookie, accessTokenResponse);
    }

    public void writeSuccessResponse(HttpServletResponse response, TokenResponse tokenResponse) throws IOException {
        long maxAge = Duration.between(Instant.now(), tokenResponse.refreshToken().expiresAt()).getSeconds();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponse.refreshToken().token())
                .maxAge(maxAge)
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        AccessTokenResponse responseBody = AccessTokenResponse.from(tokenResponse.accessToken());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
