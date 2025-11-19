package com.synapse.account_service.integrationtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.synapse.account_service.TestConfig;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.enums.MemberRole;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.service.JwtTokenService;
import com.synapse.account_service.service.TokenManagementService;
import com.synapse.account_service_api.dto.response.TokenResponse;

import jakarta.servlet.http.Cookie;

public class TokenReissueIntegrationTest extends TestConfig {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private TokenManagementService tokenManagementService;

    private Member testMember;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 및 저장
        testMember = Member.builder()
                .id(UUID.randomUUID())
                .email("reissue_user@example.com")
                .username("reissue_user")
                .password("password")
                .role(MemberRole.USER)
                .provider("local")
            .defaultWorkspaceId(UUID.randomUUID())
                .build();
        testMember = memberRepository.save(testMember);

        // 테스트용 유효한 리프레시 토큰 생성 및 Redis에 저장
        TokenResponse tokens = jwtTokenService.createTokenResponse(testMember.getId());
        validRefreshToken = tokens.refreshToken().token();
        tokenManagementService.saveOrUpdateRefreshToken(testMember.getId(), tokens.refreshToken());
    }

    @Test
    @DisplayName("토큰 재발급 API 성공: 유효한 쿠키로 요청 시, 새 토큰을 응답하고 쿠키를 갱신한다")
    void reissueApi_success() throws Exception {
        // given
        Cookie refreshTokenCookie = new Cookie("refreshToken", validRefreshToken);

        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/token/reissue")
            .cookie(refreshTokenCookie)
        );

        // then
        actions
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNumber())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("토큰 재발급 API 실패: 쿠키가 없을 경우, 400 Bad Request를 응답한다")
    void reissueApi_fail_whenCookieIsMissing() throws Exception {
        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/token/reissue"));

        // then
        // @CookieValue(required=true)에 의해 MissingCookieException이 발생하고,
        // GlobalExceptionHandler가 이를 400 Bad Request로 처리한다고 가정합니다.
        actions
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 재발급 API 실패: 만료되거나 유효하지 않은 토큰일 경우, 401 Unauthorized를 응답한다")
    void reissueApi_fail_whenTokenIsInvalid() throws Exception {
        // given
        // 만료 시간을 과거로 설정한 유효하지 않은 토큰
        String expiredToken = jwtTokenService.createExpiredTokenForTest(testMember.getId().toString());
        Cookie expiredCookie = new Cookie("refreshToken", expiredToken);

        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/token/reissue")
                .cookie(expiredCookie));

        // then
        // JwtService에서 TokenExpiredException이 발생하고,
        // GlobalExceptionHandler가 이를 401 Unauthorized로 처리한다고 가정합니다.
        actions
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
