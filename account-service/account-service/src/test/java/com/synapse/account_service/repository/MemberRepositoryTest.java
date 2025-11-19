package com.synapse.account_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.synapse.account_service.TestConfig;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.enums.MemberRole;
import com.synapse.account_service.domain.repository.MemberRepository;

import jakarta.persistence.EntityManager;

public class MemberRepositoryTest extends TestConfig {

    
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encrypted_password")
                .username("테스트유저")
                .role(MemberRole.USER)
                .provider("local")
            .defaultWorkspaceId(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("이메일로 회원을 성공적으로 조회해야 한다")
    void findByEmail_shouldReturnMember() {
        // given: 먼저 테스트할 회원을 저장합니다.
        memberRepository.save(testMember);

        // when: 저장된 이메일로 조회를 시도합니다.
        Optional<Member> foundMemberOpt = memberRepository.findByEmail("test@example.com");

        // then: 결과가 존재하고, 이메일이 일치하는지 확인합니다.
        assertThat(foundMemberOpt).isPresent();
        assertThat(foundMemberOpt.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("중복된 이메일로 회원을 저장하려고 하면, DataIntegrityViolationException이 발생해야 한다")
    void save_withDuplicateEmail_shouldThrowException() {
        // given: 첫 번째 회원을 저장하고, DB에 즉시 반영(flush)합니다.
        memberRepository.save(testMember);
        entityManager.flush(); // 영속성 컨텍스트의 변경사항을 DB에 즉시 반영
        entityManager.clear(); // 1차 캐시를 비워서, 다음 조회가 DB에서 일어나도록 강제

        // when: 동일한 이메일을 가진 새로운 회원을 만듭니다.
        Member duplicateMember = Member.builder()
                .id(UUID.randomUUID())
                .email("test@example.com") // 중복된 이메일
                .password("another_password")
                .role(MemberRole.USER)
                .provider("local")
            .defaultWorkspaceId(UUID.randomUUID())
                .build();

        // then: 이 회원을 저장하려고 할 때, DB의 unique 제약 조건에 걸려 예외가 발생하는지 확인합니다.
        assertThrows(DataIntegrityViolationException.class, () -> {
            memberRepository.saveAndFlush(duplicateMember); // DB 제약조건을 바로 확인하기 위해 flush 사용
        });
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면, 비어있는 Optional을 반환해야 한다")
    void findByEmail_withNonExistentEmail_shouldReturnEmpty() {
        // when: 존재하지 않는 이메일로 조회를 시도합니다.
        Optional<Member> foundMemberOpt = memberRepository.findByEmail("not-exist@example.com");

        // then: 결과가 비어있는지 확인합니다.
        assertThat(foundMemberOpt).isEmpty();
    }

    @Test
    @DisplayName("엔티티 저장 시 생성 날짜(createdAt)가 자동으로 설정되어야 한다")
    void save_shouldSetCreatedAt() {
        // given
        
        // when
        Member savedMember = memberRepository.saveAndFlush(testMember);
        
        // then
        assertThat(savedMember.getCreatedDate()).isNotNull();
        assertThat(savedMember.getUpdatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Provider와 Provider ID로 회원을 성공적으로 조회해야 한다 (OAuth2)")
    void findByProviderAndRegistrationId_shouldReturnMember() {
        // given
        Member oauthMember = Member.builder()
                .id(UUID.randomUUID())
                .email("google_user@example.com")
                .password("social_login_password") // 실제로는 비밀번호가 없을 수도 있습니다.
                .username("구글유저")
                .role(MemberRole.USER)
                .provider("google") // 소셜 로그인 제공자
                .registrationId("1234567890") // 제공자가 부여한 고유 ID
            .defaultWorkspaceId(UUID.randomUUID())
                .build();
        memberRepository.save(oauthMember);

        // when
        Optional<Member> foundMemberOpt = memberRepository.findByProviderAndRegistrationId("google", "1234567890");

        // then
        assertThat(foundMemberOpt).isPresent();
        assertThat(foundMemberOpt.get().getEmail()).isEqualTo("google_user@example.com");
    }
}
