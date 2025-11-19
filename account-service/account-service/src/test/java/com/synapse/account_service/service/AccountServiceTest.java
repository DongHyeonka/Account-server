package com.synapse.account_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.eventuate.publisher.MemberDomainEventPublisher;
import com.synapse.account_service.eventuate.publisher.SubscriptionDomainEventPublisher;
import com.synapse.account_service_api.dto.request.SignUpRequest;
import com.synapse.account_service_api.dto.response.SignUpResponse;
import com.synapse.account_service.exception.DuplicatedException;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberDomainEventPublisher memberDomainEventPublisher;

    @Mock
    private SubscriptionDomainEventPublisher subscriptionDomainEventPublisher;

    @Mock
    private DefaultWorkspaceService defaultWorkspaceService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        // given: 테스트 준비
        SignUpRequest request = new SignUpRequest("test@example.com", "테스트유저", "password123");
        
        given(memberRepository.findByUsernameAndEmail(anyString(), anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(defaultWorkspaceService.createDefaultWorkspaceId(any(UUID.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member memberToSave = invocation.getArgument(0);
            return memberToSave;
        });
        
        // when: 실제 테스트할 메서드 호출
        SignUpResponse response = accountService.registerMember(request);
        
        // then: 결과 검증
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.username()).isEqualTo("테스트유저");
        
        verify(passwordEncoder).encode("password123");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("이메일 또는 사용자명 중복으로 회원가입 실패")
    void signUp_fail_withDuplicateUsernameAndEmail() {
        // given
        SignUpRequest request = new SignUpRequest("test@example.com", "테스트유저", "password123");
        
        // memberRepository.findByUsernameAndEmail이 호출되면, 이미 존재하는 Member 객체를 반환하도록 설정
        given(memberRepository.findByUsernameAndEmail(anyString(), anyString())).willReturn(Optional.of(
            Member.builder().defaultWorkspaceId(UUID.randomUUID()).build()
        ));
        
        // when & then: DuplicatedException이 발생하는지 검증
        assertThrows(DuplicatedException.class, () -> {
            accountService.registerMember(request);
        });
    }
}
