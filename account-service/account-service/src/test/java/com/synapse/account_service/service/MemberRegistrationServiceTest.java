package com.synapse.account_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.synapse.account_service.domain.Attributes;
import com.synapse.account_service.domain.ProviderUser;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.domain.socials.GoogleUser;
import com.synapse.account_service.eventuate.publisher.MemberDomainEventPublisher;
import com.synapse.account_service.eventuate.publisher.SubscriptionDomainEventPublisher;

@ExtendWith(MockitoExtension.class)
class MemberRegistrationServiceTest {

    @InjectMocks
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberDomainEventPublisher memberDomainEventPublisher;

    @Mock
    private SubscriptionDomainEventPublisher subscriptionDomainEventPublisher;

    @Mock
    private DefaultWorkspaceService defaultWorkspaceService;

    private final String provider = "google";
    private final String providerId = "123456789";
    private final String email = "test@example.com";
    private final String username = "testuser";

    private ProviderUser createMockProviderUser() {
        Map<String, Object> attributes = Map.of("sub", providerId, "email", email, "name", username);
        var oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");
        return new GoogleUser(new Attributes(attributes), oAuth2User, null); // ClientRegistration은 이 테스트에서 사용되지 않음
    }

    @Test
    @DisplayName("시나리오 1: 신규 사용자일 경우, 새 Member를 생성하고 저장한다")
    void registerOauthUser_whenNewUser_createsAndSavesMember() {
        // given
        ProviderUser providerUser = createMockProviderUser();
        given(memberRepository.findBySocialIdOrEmailOrUsername(provider, providerId, email, username))
                .willReturn(Optional.empty());

        // registerOauthUser 내부에서 save가 호출될 때 반환할 Member 객체를 준비
        Member savedMember = Member.builder()
            .id(UUID.randomUUID())
            .email(email)
            .username(username)
            .provider(provider)
            .registrationId(providerId)
            .defaultWorkspaceId(UUID.randomUUID())
            .build();
        given(defaultWorkspaceService.createDefaultWorkspaceId(any(UUID.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // when
        Member result = memberRegistrationService.registerOauthUser(provider, providerUser);

        // then
        verify(memberRepository, times(1)).save(any(Member.class));
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getProvider()).isEqualTo(provider);
        assertThat(result.getRegistrationId()).isEqualTo(providerId);
    }

    @Test
    @DisplayName("시나리오 2: 이메일/이름이 일치하는 기존 사용자일 경우, 소셜 정보를 연동한다")
    void registerOauthUser_whenExistingUser_linksSocialAccount() {
        // given
        ProviderUser providerUser = createMockProviderUser();
        
        // 소셜 정보는 없고, 이메일만 있는 기존 Member Mock
        Member existingMember = mock(Member.class);
        given(memberRepository.findBySocialIdOrEmailOrUsername(provider, providerId, email, username))
                .willReturn(Optional.of(existingMember));
        
        // getProvider()가 null을 반환하도록 설정하여 연동 조건 만족
        given(existingMember.getProvider()).willReturn(null);

        // when
        Member result = memberRegistrationService.registerOauthUser(provider, providerUser);

        // then
        verify(memberRepository, never()).save(any(Member.class)); // save는 호출되면 안 됨
        verify(existingMember, times(1)).linkSocialAccount(provider, providerId); // linkSocialAccount는 호출되어야 함
        assertThat(result).isEqualTo(existingMember);
    }

    @Test
    @DisplayName("시나리오 3: 소셜 정보가 이미 등록된 사용자일 경우, 아무 작업 없이 반환한다")
    void registerOauthUser_whenSocialAccountExists_returnsMember() {
        // given
        ProviderUser providerUser = createMockProviderUser();
        
        // 소셜 정보가 이미 있는 기존 Member Mock
        Member existingMember = mock(Member.class);
        given(memberRepository.findBySocialIdOrEmailOrUsername(provider, providerId, email, username))
                .willReturn(Optional.of(existingMember));

        // getProvider()와 getRegistrationId()가 모두 값을 반환하도록 설정하여 연동 조건 불만족
        given(existingMember.getProvider()).willReturn(provider);
        given(existingMember.getRegistrationId()).willReturn(providerId);

        // when
        Member result = memberRegistrationService.registerOauthUser(provider, providerUser);

        // then
        verify(memberRepository, never()).save(any(Member.class));
        verify(existingMember, never()).linkSocialAccount(anyString(), anyString());
        assertThat(result).isEqualTo(existingMember);
    }
}
