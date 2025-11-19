package com.synapse.account_service.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.entity.Subscription;
import com.synapse.account_service.domain.enums.SubscriptionTier;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.configuration.SynapseJwtProperties;
import com.synapse.account_service.exception.ExceptionType;
import com.synapse.account_service.exception.JWTTokenExpiredException;
import com.synapse.account_service.exception.JWTValidationException;
import com.synapse.account_service.exception.NotFoundException;
import com.synapse.account_service_api.dto.TokenResult;
import com.synapse.account_service_api.dto.response.TokenResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtTokenTemplate jwtTokenTemplate;
    private final SynapseJwtProperties jwtProperties;
    private final MemberRepository memberRepository;
    private final DefaultWorkspaceService defaultWorkspaceService;

    @Transactional(readOnly = true)
    public TokenResponse createTokenResponse(UUID memberId) {
        Member member = memberRepository.findByIdWithSubscription(memberId)
            .orElseThrow(() -> new NotFoundException(ExceptionType.NOT_FOUND_MEMBER));

        Map<String, Object> accessTokenClaims = buildAccessTokenClaims(member);

        TokenResult accessToken = jwtTokenTemplate.createToken(
            memberId.toString(),
            accessTokenClaims,
            jwtProperties.getAccessTokenTtl(),
            jwtProperties.getAudience().getAccess());

        TokenResult refreshToken = jwtTokenTemplate.createRefreshToken(
            memberId.toString(),
            jwtProperties.getRefreshTokenTtl(),
            jwtProperties.getAudience().getRefresh(),
            UUID.randomUUID().toString());

        return new TokenResponse(accessToken, refreshToken);
    }

    public UUID getMemberIdFromAccessToken(String token) {
        return getMemberIdFrom(token, TokenCategory.ACCESS, ExceptionType.INVALID_ACCESS_TOKEN);
    }
    
    public UUID getMemberIdFromRefreshToken(String token) {
        return getMemberIdFrom(token, TokenCategory.REFRESH, ExceptionType.INVALID_REFRESH_TOKEN);
    }

    private UUID getMemberIdFrom(String token, TokenCategory category, ExceptionType invalidTokenType) {
        try {
            DecodedJWT decodedJWT = switch (category) {
                case ACCESS -> jwtTokenTemplate.verifyAccessToken(token);
                case REFRESH -> jwtTokenTemplate.verifyRefreshToken(token);
            };
            return UUID.fromString(decodedJWT.getSubject());
        } catch (TokenExpiredException e) {
            throw new JWTTokenExpiredException(ExceptionType.EXPIRED_TOKEN);
        } catch (JWTVerificationException e) {
            throw new JWTValidationException(invalidTokenType);
        }
    }

    private enum TokenCategory {
        ACCESS, REFRESH
    }

    // 테스트용 메서드
    public String createExpiredTokenForTest(String subject) {
        return jwtTokenTemplate.createExpiredTokenForTest(subject);
    }

    private Map<String, Object> buildAccessTokenClaims(Member member) {
        String workspaceId = defaultWorkspaceService.getDefaultWorkspaceId(member).toString();
        String tier = Optional.ofNullable(member.getSubscription())
            .map(Subscription::getTier)
            .orElse(SubscriptionTier.FREE)
            .getGatewayValue();

        return Map.of(
            "workspace_id", workspaceId,
            "tier", tier,
            "role", member.getRole().getGatewayValue(),
            "username", member.getUsername()
        );
    }
}
