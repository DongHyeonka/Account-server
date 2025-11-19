package com.synapse.account_service.service.handler;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.synapse.account_service.domain.PrincipalUser;
import com.synapse.account_service.service.JwtTokenService;
import com.synapse.account_service.service.TokenManagementService;
import com.synapse.account_service.utils.AuthResponseWriter;
import com.synapse.account_service_api.dto.response.TokenResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenService jwtTokenService;
    private final TokenManagementService tokenManagementService;
    private final AuthResponseWriter authResponseWriter;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();
        if (principalUser.member() == null) {
            throw new InternalAuthenticationServiceException("회원 정보를 찾을 수 없습니다.");
        }

        UUID memberId = principalUser.member().getId();
        
        TokenResponse tokenResponse = jwtTokenService.createTokenResponse(memberId);

        tokenManagementService.saveOrUpdateRefreshToken(memberId, tokenResponse.refreshToken());

        authResponseWriter.writeSuccessResponse(response, tokenResponse);
    }
}
