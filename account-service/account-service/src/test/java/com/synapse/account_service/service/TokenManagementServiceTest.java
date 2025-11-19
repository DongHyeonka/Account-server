package com.synapse.account_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.synapse.account_service.domain.RefreshToken;
import com.synapse.account_service.exception.JWTValidationException;
import com.synapse.account_service_api.dto.TokenResult;
import com.synapse.account_service_api.dto.response.TokenResponse;

@ExtendWith(MockitoExtension.class)
class TokenManagementServiceTest {

    @InjectMocks
    private TokenManagementService tokenManagementService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RedisTemplate<String, RefreshToken> refreshTokenRedisTemplate;

    @Mock
    private ValueOperations<String, RefreshToken> valueOperations;

    private UUID memberId;
    private String oldRefreshToken;
    private String newRefreshToken;
    private String redisKey;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        oldRefreshToken = "old-refresh-token";
        newRefreshToken = "new-refresh-token";
        redisKey = "refresh_token:" + memberId.toString();
    }

    @Test
    @DisplayName("토큰 재발급 시 Rotation 정책 동작 확인 (새 토큰 저장)")
    void reissueTokens_rotatesToken() {
        // given
        given(refreshTokenRedisTemplate.opsForValue()).willReturn(valueOperations);
        RefreshToken storedToken = new RefreshToken(memberId, oldRefreshToken);
        given(jwtTokenService.getMemberIdFromRefreshToken(oldRefreshToken)).willReturn(memberId);
        given(valueOperations.get(redisKey)).willReturn(storedToken);

        TokenResult newAccess = new TokenResult("new-access", Instant.now().plusSeconds(3600));
        TokenResult newRefresh = new TokenResult(newRefreshToken, Instant.now().plusSeconds(1209600)); // 14 days
        TokenResponse tokenResponse = new TokenResponse(newAccess, newRefresh);

        given(jwtTokenService.createTokenResponse(memberId)).willReturn(tokenResponse);

        // when
        TokenResponse result = tokenManagementService.reissueTokens(oldRefreshToken);

        // then
        assertThat(result.refreshToken().token()).isEqualTo(newRefreshToken);

        // Verify Redis update (Rotation)
        verify(valueOperations).set(eq(redisKey), any(RefreshToken.class), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("토큰 재발급 시 기존 토큰 불일치(탈취 의심) 시 전체 무효화")
    void reissueTokens_detectsReuse() {
        // given
        given(refreshTokenRedisTemplate.opsForValue()).willReturn(valueOperations);
        String stolenToken = "stolen-token";
        RefreshToken storedToken = new RefreshToken(memberId, "different-token"); // Redis has newer token

        given(jwtTokenService.getMemberIdFromRefreshToken(stolenToken)).willReturn(memberId);
        given(valueOperations.get(redisKey)).willReturn(storedToken);

        // when & then
        assertThatThrownBy(() -> tokenManagementService.reissueTokens(stolenToken))
                .isInstanceOf(JWTValidationException.class);

        // Verify invalidation
        verify(refreshTokenRedisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("로그아웃 시 Redis에서 토큰 삭제 확인")
    void logout_removesToken() {
        // given
        given(jwtTokenService.getMemberIdFromRefreshToken(oldRefreshToken)).willReturn(memberId);

        // when
        tokenManagementService.logout(oldRefreshToken);

        // then
        verify(refreshTokenRedisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("RefreshToken 저장 시 TTL 설정 확인")
    void saveOrUpdateRefreshToken_setsTTL() {
        // given
        given(refreshTokenRedisTemplate.opsForValue()).willReturn(valueOperations);
        TokenResult tokenResult = new TokenResult(newRefreshToken, Instant.now().plusSeconds(1209600)); // 14 days

        // when
        tokenManagementService.saveOrUpdateRefreshToken(memberId, tokenResult);

        // then
        verify(valueOperations).set(eq(redisKey), any(RefreshToken.class), anyLong(), eq(TimeUnit.SECONDS)); // Approx
                                                                                                             // check
    }
}
