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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.synapse.account_service.TestConfig;
import com.synapse.account_service.convert.ProviderUserConverter;
import com.synapse.account_service.convert.ProviderUserRequest;
import com.synapse.account_service.domain.Attributes;
import com.synapse.account_service.domain.PrincipalUser;
import com.synapse.account_service.domain.ProviderUser;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.socials.GoogleUser;
import com.synapse.account_service.service.CustomOAuth2UserService;
import com.synapse.account_service.service.MemberRegistrationService;

public class CustomOAuth2UserServiceTest extends TestConfig {
    
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private MemberRegistrationService registrationService;
    
    @Mock
    private ProviderUserConverter<ProviderUserRequest, ProviderUser> providerUserConverter;
    
    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;


    private String userEmail = "google_user@example.com";
    private String username = "구글유저";
    private String providerId = "1234567890";

    @BeforeEach
    void setUp() {
        customOAuth2UserService = new CustomOAuth2UserService(oAuth2UserService);
        ReflectionTestUtils.setField(customOAuth2UserService, "memberRegistrationService", registrationService);
        ReflectionTestUtils.setField(customOAuth2UserService, "providerUserConverter", providerUserConverter);
    }

    @Test
    @DisplayName("소셜 로그인 시 회원가입/로그인 처리 통합 테스트")
    void loadUser_registersOrLogsIn() {
        // given
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-id")
                .userNameAttributeName("sub")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("uri")
                .tokenUri("uri")
                .authorizationUri("uri")
                .userInfoUri("uri")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token", Instant.now(), Instant.now().plusSeconds(60));
        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);
        
        Map<String, Object> attributes = Map.of("sub", providerId, "name", username, "email", userEmail);
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");
        
        ProviderUser mockProviderUser = new GoogleUser(new Attributes(attributes), mockOAuth2User, clientRegistration);
        
        Member mockMember = Member.builder()
            .email(userEmail)
            .username(username)
            .defaultWorkspaceId(UUID.randomUUID())
            .build();

        given(oAuth2UserService.loadUser(any(OAuth2UserRequest.class))).willReturn(mockOAuth2User);
        given(providerUserConverter.convert(any(ProviderUserRequest.class))).willReturn(mockProviderUser);
        given(registrationService.registerOauthUser("google", mockProviderUser)).willReturn(mockMember);

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        verify(oAuth2UserService, times(1)).loadUser(any(OAuth2UserRequest.class));
        verify(providerUserConverter, times(1)).convert(any(ProviderUserRequest.class));
        verify(registrationService, times(1)).registerOauthUser("google", mockProviderUser);
        
        assertThat(result).isInstanceOf(PrincipalUser.class);
        assertThat(((PrincipalUser) result).getUsername()).isEqualTo(username); 
        assertThat(((PrincipalUser) result).providerUser().getUsername()).isEqualTo(username);
    }
}
