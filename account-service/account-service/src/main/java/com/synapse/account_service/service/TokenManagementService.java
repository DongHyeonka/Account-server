package com.synapse.account_service.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.account_service.domain.RefreshToken;
import com.synapse.account_service.exception.ExceptionType;
import com.synapse.account_service.exception.JWTValidationException;
import com.synapse.account_service_api.dto.TokenResult;
import com.synapse.account_service_api.dto.response.TokenResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TokenManagementService {
    private final JwtTokenService jwtTokenService;
    private final RedisTemplate<String, RefreshToken> refreshTokenRedisTemplate;

    public void saveOrUpdateRefreshToken(UUID memberId, TokenResult refreshToken) {
        String redisKey = "refresh_token:" + memberId.toString();
        RefreshToken refreshTokenEntity = new RefreshToken(memberId, refreshToken.token());
        
        long ttlSeconds = ChronoUnit.SECONDS.between(Instant.now(), refreshToken.expiresAt());
        refreshTokenRedisTemplate.opsForValue().set(redisKey, refreshTokenEntity, ttlSeconds, TimeUnit.SECONDS);
    }

    public TokenResponse reissueTokens(String requestRefreshToken) {
        UUID memberId = jwtTokenService.getMemberIdFromRefreshToken(requestRefreshToken);
        String redisKey = "refresh_token:" + memberId.toString();

        RefreshToken storedToken = refreshTokenRedisTemplate.opsForValue().get(redisKey);
        if (storedToken == null) {
            throw new JWTValidationException(ExceptionType.INVALID_REFRESH_TOKEN);
        }

        if (!storedToken.getToken().equals(requestRefreshToken)) {
            refreshTokenRedisTemplate.delete(redisKey);
            throw new JWTValidationException(ExceptionType.TAMPERED_REFRESH_TOKEN);
        }

        TokenResponse newTokens = jwtTokenService.createTokenResponse(memberId);

        // Redis에 새로운 RefreshToken 저장
        RefreshToken newRefreshToken = new RefreshToken(memberId, newTokens.refreshToken().token());
        long ttlSeconds = Duration.between(Instant.now(), newTokens.refreshToken().expiresAt()).getSeconds();
        refreshTokenRedisTemplate.opsForValue().set(redisKey, newRefreshToken, ttlSeconds, TimeUnit.SECONDS);

        return newTokens;
    }
}
