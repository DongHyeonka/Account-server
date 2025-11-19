package com.synapse.account_service.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.synapse.account_service.TestConfig;
import com.synapse.account_service.convert.ProviderUserConverter;
import com.synapse.account_service.convert.ProviderUserRequest;
import com.synapse.account_service.domain.Attributes;
import com.synapse.account_service.domain.PrincipalUser;
import com.synapse.account_service.domain.ProviderUser;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.socials.KakaoOidcUser;
import com.synapse.account_service.service.CustomOidcUserService;
import com.synapse.account_service.service.MemberRegistrationService;

public class CustomOidcUserServiceTest extends TestConfig {

    private CustomOidcUserService customOidcUserService;

    @Mock
    private MemberRegistrationService registrationService;

    @Mock
    private ProviderUserConverter<ProviderUserRequest, ProviderUser> providerUserConverter;

    @Mock
    private OidcUserService oidcUserService;

    private String userEmail = "kakao_user@example.com";
    private String username = "카카오유저";
    private String providerId = "1234567890";

    @BeforeEach
    void setUp() {
        customOidcUserService = new CustomOidcUserService(oidcUserService);
        ReflectionTestUtils.setField(customOidcUserService, "memberRegistrationService", registrationService);
        ReflectionTestUtils.setField(customOidcUserService, "providerUserConverter", providerUserConverter);
    }

    @Test
    @DisplayName("OIDC 소셜 로그인 시 회원가입/로그인 처리 통합 테스트")
    void loadUser_registersOrLogsIn() {
        // given
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("uri")
                .tokenUri("uri")
                .authorizationUri("uri")
                .userInfoUri("uri")
                .build();

        Map<String, Object> claims = Map.of("sub", providerId, "nickname", username, "email", userEmail);
        OidcIdToken idToken = new OidcIdToken("test-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token",
                Instant.now(), Instant.now().plusSeconds(60));
        OidcUserRequest userRequest = new OidcUserRequest(clientRegistration, accessToken, idToken);

        OidcUser mockOidcUser = new DefaultOidcUser(Collections.emptyList(), idToken, "sub");

        ProviderUser mockProviderUser = new KakaoOidcUser(new Attributes(claims), mockOidcUser, clientRegistration);

        Member mockMember = Member.builder()
                .email(userEmail)
                .username(username)
                .defaultWorkspaceId(UUID.randomUUID())
                .build();

        given(oidcUserService.loadUser(any(OidcUserRequest.class))).willReturn(mockOidcUser);
        given(providerUserConverter.convert(any(ProviderUserRequest.class))).willReturn(mockProviderUser);
        given(registrationService.registerOauthUser("kakao", mockProviderUser)).willReturn(mockMember);

        // when
        OidcUser result = customOidcUserService.loadUser(userRequest);

        // then
        verify(oidcUserService, times(1)).loadUser(any(OidcUserRequest.class));
        verify(providerUserConverter, times(1)).convert(any(ProviderUserRequest.class));
        verify(registrationService, times(1)).registerOauthUser("kakao", mockProviderUser);

        assertThat(result).isInstanceOf(PrincipalUser.class);
        assertThat(((PrincipalUser) result).getUsername()).isEqualTo(username);
        assertThat(((PrincipalUser) result).providerUser().getUsername()).isEqualTo(username);

        // Verify OIDC methods
        assertThat(result.getClaims()).containsEntry("sub", providerId);
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getClaims()).containsEntry("sub", providerId);
        assertThat(result.getIdToken()).isNull();
    }
}
