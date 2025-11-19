package com.synapse.account_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.synapse.account_service.configuration.SynapseJwtProperties;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.entity.Subscription;
import com.synapse.account_service.domain.enums.MemberRole;
import com.synapse.account_service.domain.enums.SubscriptionTier;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.exception.ExceptionType;
import com.synapse.account_service_api.dto.TokenResult;
import com.synapse.account_service_api.dto.response.TokenResponse;

@ExtendWith(MockitoExtension.class)
public class JwtTokenServiceTest {

    @InjectMocks
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtTokenTemplate jwtTokenTemplate;

    @Mock
    private SynapseJwtProperties jwtProperties;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DefaultWorkspaceService defaultWorkspaceService;

    private SynapseJwtProperties testJwtProperties;
    private JwtTokenTemplate testJwtTokenTemplate;

    @BeforeEach
    void setUp() {
        testJwtProperties = createJwtProperties();
        testJwtTokenTemplate = new JwtTokenTemplate(testJwtProperties);
    }

    @Test
    @DisplayName("토큰 생성 서비스 성공: 올바른 인자로 토큰 생성을 요청하고 DTO를 반환한다")
    void createTokenResponse_success() {
        // given
        SynapseJwtProperties.Audience audience = new SynapseJwtProperties.Audience();
        audience.setAccess("synapse-gateway");
        audience.setRefresh("synapse-account");

        given(jwtProperties.getAudience()).willReturn(audience);
        Duration accessTtl = Duration.ofMinutes(30);
        Duration refreshTtl = Duration.ofDays(1);
        given(jwtProperties.getAccessTokenTtl()).willReturn(accessTtl);
        given(jwtProperties.getRefreshTokenTtl()).willReturn(refreshTtl);

        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Member member = Member.builder()
            .id(memberId)
            .username("test-user")
            .password("password")
            .email("test@example.com")
            .provider("local")
            .role(MemberRole.USER)
            .defaultWorkspaceId(workspaceId)
            .build();

        Subscription subscription = Subscription.builder()
            .id(UUID.randomUUID())
            .member(member)
            .tier(SubscriptionTier.PRO)
            .nextRenewalDate(ZonedDateTime.now().plusDays(30))
            .build();
        member.setSubscription(subscription);

        given(memberRepository.findByIdWithSubscription(memberId)).willReturn(java.util.Optional.of(member));
        given(defaultWorkspaceService.getDefaultWorkspaceId(member)).willReturn(workspaceId);

        TokenResult mockAccessToken = new TokenResult("access.token.string", Instant.now().plusSeconds(1800));
        TokenResult mockRefreshToken = new TokenResult("refresh.token.string", Instant.now().plusSeconds(86400));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);

        given(jwtTokenTemplate.createToken(eq(memberId.toString()), claimsCaptor.capture(), eq(accessTtl), eq(audience.getAccess())))
            .willReturn(mockAccessToken);
        given(jwtTokenTemplate.createRefreshToken(eq(memberId.toString()), eq(refreshTtl), eq(audience.getRefresh()), any()))
            .willReturn(mockRefreshToken);

        // when
        TokenResponse tokenResponse = jwtTokenService.createTokenResponse(memberId);

        // then: 결과 검증
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.accessToken().token()).isEqualTo("access.token.string");
        assertThat(tokenResponse.refreshToken().token()).isEqualTo("refresh.token.string");

        verify(jwtTokenTemplate, times(1)).createToken(eq(memberId.toString()), any(), eq(accessTtl), eq(audience.getAccess()));
        verify(jwtTokenTemplate, times(1)).createRefreshToken(eq(memberId.toString()), eq(refreshTtl), eq(audience.getRefresh()), any());

        Map<String, Object> capturedClaims = claimsCaptor.getValue();
        assertThat(capturedClaims).containsEntry("workspace_id", workspaceId.toString())
            .containsEntry("tier", SubscriptionTier.PRO.getGatewayValue())
            .containsEntry("role", MemberRole.USER.getGatewayValue())
            .containsEntry("username", member.getUsername());
    }

    @Test
    @DisplayName("Access Token은 스펙에 정의된 모든 클레임을 포함한다")
    void createAccessToken_containsAllGatewayClaims() {
        // given
        JwtTokenService service = new JwtTokenService(testJwtTokenTemplate, testJwtProperties, memberRepository, defaultWorkspaceService);

        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Member member = buildMember(memberId, "access-user", MemberRole.ADMIN, workspaceId, SubscriptionTier.PRO);

        given(memberRepository.findByIdWithSubscription(memberId)).willReturn(Optional.of(member));
        given(defaultWorkspaceService.getDefaultWorkspaceId(member)).willReturn(workspaceId);

        // when
        TokenResponse tokenResponse = service.createTokenResponse(memberId);
        DecodedJWT decodedAccessToken = testJwtTokenTemplate.verifyAccessToken(tokenResponse.accessToken().token());

        // then
        assertGatewayAccessClaims(decodedAccessToken, testJwtProperties, memberId, workspaceId, SubscriptionTier.PRO, MemberRole.ADMIN, member.getUsername());
    }

    @Test
    @DisplayName("Refresh Token은 필수 클레임(iss/aud/sub/exp)을 충족한다")
    void createRefreshToken_containsRequiredClaims() {
        // given
        JwtTokenService service = new JwtTokenService(testJwtTokenTemplate, testJwtProperties, memberRepository, defaultWorkspaceService);

        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Member member = buildMember(memberId, "refresh-user", MemberRole.USER, workspaceId, SubscriptionTier.FREE);

        given(memberRepository.findByIdWithSubscription(memberId)).willReturn(Optional.of(member));
        given(defaultWorkspaceService.getDefaultWorkspaceId(member)).willReturn(workspaceId);

        // when
        TokenResponse tokenResponse = service.createTokenResponse(memberId);
        DecodedJWT decodedRefreshToken = testJwtTokenTemplate.verifyRefreshToken(tokenResponse.refreshToken().token());

        // then
        assertRefreshTokenClaims(decodedRefreshToken, testJwtProperties, memberId);
    }

    @Test
    @DisplayName("멤버 정보가 변경되면 새로운 토큰에 최신 클레임이 반영된다")
    void createTokenResponse_reflectsLatestTierRoleWorkspace() {
        // given
        JwtTokenService service = new JwtTokenService(testJwtTokenTemplate, testJwtProperties, memberRepository, defaultWorkspaceService);

        UUID memberId = UUID.randomUUID();
        UUID initialWorkspaceId = UUID.randomUUID();
        UUID updatedWorkspaceId = UUID.randomUUID();

        Member initialMember = buildMember(memberId, "rolling-user", MemberRole.USER, initialWorkspaceId, SubscriptionTier.FREE);
        Member updatedMember = buildMember(memberId, "rolling-user", MemberRole.ADMIN, updatedWorkspaceId, SubscriptionTier.PRO);

        given(memberRepository.findByIdWithSubscription(memberId))
            .willReturn(Optional.of(initialMember))
            .willReturn(Optional.of(updatedMember));
        given(defaultWorkspaceService.getDefaultWorkspaceId(initialMember)).willReturn(initialWorkspaceId);
        given(defaultWorkspaceService.getDefaultWorkspaceId(updatedMember)).willReturn(updatedWorkspaceId);

        // when
        TokenResponse initialResponse = service.createTokenResponse(memberId);
        TokenResponse updatedResponse = service.createTokenResponse(memberId);

        DecodedJWT initialToken = testJwtTokenTemplate.verifyAccessToken(initialResponse.accessToken().token());
        DecodedJWT updatedToken = testJwtTokenTemplate.verifyAccessToken(updatedResponse.accessToken().token());

        // then
        assertGatewayAccessClaims(initialToken, testJwtProperties, memberId, initialWorkspaceId, SubscriptionTier.FREE, MemberRole.USER, initialMember.getUsername());
        assertGatewayAccessClaims(updatedToken, testJwtProperties, memberId, updatedWorkspaceId, SubscriptionTier.PRO, MemberRole.ADMIN, updatedMember.getUsername());
    }

    @Test
    @DisplayName("Refresh 토큰 검증 시 잘못된 audience면 INVALID_REFRESH_TOKEN 예외를 던진다")
    void getMemberIdFromRefreshToken_rejectsAccessToken() {
        // given
        JwtTokenService service = new JwtTokenService(testJwtTokenTemplate, testJwtProperties, memberRepository, defaultWorkspaceService);

        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Member member = buildMember(memberId, "aud-user", MemberRole.USER, workspaceId, SubscriptionTier.FREE);

        given(memberRepository.findByIdWithSubscription(memberId)).willReturn(Optional.of(member));
        given(defaultWorkspaceService.getDefaultWorkspaceId(member)).willReturn(workspaceId);

        TokenResponse tokenResponse = service.createTokenResponse(memberId);

        // when / then
        assertThatThrownBy(() -> service.getMemberIdFromRefreshToken(tokenResponse.accessToken().token()))
            .isInstanceOf(com.synapse.account_service.exception.JWTValidationException.class)
            .hasMessage(ExceptionType.INVALID_REFRESH_TOKEN.getMessage());

    }

    private SynapseJwtProperties createJwtProperties() {
        SynapseJwtProperties properties = new SynapseJwtProperties();
        properties.setIssuer("account-server");

        SynapseJwtProperties.Audience audience = new SynapseJwtProperties.Audience();
        audience.setAccess("api-gateway");
        audience.setRefresh("account-server");
        properties.setAudience(audience);

        properties.setSecret("test-secret-for-jwt-token-service");
        properties.setAccessTokenTtlMinutes(30);
        properties.setRefreshTokenTtlDays(7);
        return properties;
    }

    private Member buildMember(UUID memberId, String username, MemberRole role, UUID workspaceId, SubscriptionTier tier) {
        Member member = Member.builder()
            .id(memberId)
            .username(username)
            .password("password")
            .email(username + "@example.com")
            .provider("local")
            .role(role)
            .defaultWorkspaceId(workspaceId)
            .build();

        Subscription subscription = Subscription.builder()
            .id(UUID.randomUUID())
            .member(member)
            .tier(tier)
            .nextRenewalDate(ZonedDateTime.now().plusDays(30))
            .build();
        member.setSubscription(subscription);
        return member;
    }

    private void assertGatewayAccessClaims(
        DecodedJWT decodedJWT,
        SynapseJwtProperties properties,
        UUID memberId,
        UUID workspaceId,
        SubscriptionTier tier,
        MemberRole role,
        String username
    ) {
        assertThat(decodedJWT.getIssuer()).isEqualTo(properties.getIssuer());
        assertThat(decodedJWT.getAudience()).containsExactly(properties.getAudience().getAccess());
        assertThat(decodedJWT.getSubject()).isEqualTo(memberId.toString());

        Instant issuedAt = decodedJWT.getIssuedAt().toInstant();
        Instant expiresAt = decodedJWT.getExpiresAt().toInstant();
        assertThat(expiresAt).isEqualTo(issuedAt.plus(properties.getAccessTokenTtl()));

        assertThat(decodedJWT.getClaim("workspace_id").asString()).isEqualTo(workspaceId.toString());
        assertThat(decodedJWT.getClaim("tier").asString()).isEqualTo(tier.getGatewayValue());
        assertThat(decodedJWT.getClaim("role").asString()).isEqualTo(role.getGatewayValue());
        assertThat(decodedJWT.getClaim("username").asString()).isEqualTo(username);
    }

    private void assertRefreshTokenClaims(DecodedJWT decodedJWT, SynapseJwtProperties properties, UUID memberId) {
        assertThat(decodedJWT.getIssuer()).isEqualTo(properties.getIssuer());
        assertThat(decodedJWT.getAudience()).containsExactly(properties.getAudience().getRefresh());
        assertThat(decodedJWT.getSubject()).isEqualTo(memberId.toString());

        Instant issuedAt = decodedJWT.getIssuedAt().toInstant();
        Instant expiresAt = decodedJWT.getExpiresAt().toInstant();
        assertThat(expiresAt).isEqualTo(issuedAt.plus(properties.getRefreshTokenTtl()));
    }
}
