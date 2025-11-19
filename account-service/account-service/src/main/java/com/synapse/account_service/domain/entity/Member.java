package com.synapse.account_service.domain.entity;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.synapse.account_service.domain.common.BaseEntity;
import com.synapse.account_service.domain.enums.MemberRole;
import com.synapse.account_service_api.event.MemberDomainEvent;
import com.synapse.account_service_api.event.MemberRegisteredEvent;
import com.synapse.account_service_api.event.SocialAccountLinkedEvent;

import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members", indexes = {
    @Index(name = "idx_member_username", columnList = "username")
})
public class Member extends BaseEntity {
    @Id
    @Column(name = "member_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "provider", length = 10)
    private String provider;

    @Column(name = "picture", columnDefinition = "TEXT")
    private String picture;

    @Column(name = "registration_id")
    private String registrationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.USER;

    @Column(name = "default_workspace_id", columnDefinition = "uuid", nullable = false)
    private UUID defaultWorkspaceId;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Subscription subscription;

    @Builder
    public Member(UUID id, String username, String password, String email, String provider, String picture,
                  String registrationId, MemberRole role, UUID defaultWorkspaceId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.provider = provider;
        this.picture = picture;
        this.registrationId = registrationId;
        this.role = role;
        this.defaultWorkspaceId = defaultWorkspaceId;
    }

    public static ResultWithDomainEvents<Member, MemberDomainEvent> register(
        UUID userId, String email, String username, String encodedPassword, String provider, String registrationId
    ) {
        Member member = Member.builder()
                .id(userId)
                .email(email)
                .username(username)
                .password(encodedPassword)
                .provider(provider)
                .registrationId(registrationId)
                .role(MemberRole.USER)
                .build();
        
        MemberRegisteredEvent memberEvent = new MemberRegisteredEvent(userId, email, username);

        return new ResultWithDomainEvents<>(member, memberEvent);
    }

    public static ResultWithDomainEvents<Member, MemberDomainEvent> linkSocialAccount(
        UUID userId, String provider, String registrationId
    ) {
        Member member = Member.builder()
                .id(userId)
                .provider(provider)
                .registrationId(registrationId)
                .role(MemberRole.USER)
                .defaultWorkspaceId(userId)
                .build();
        
        SocialAccountLinkedEvent socialEvent = new SocialAccountLinkedEvent(userId, provider, registrationId);

        return new ResultWithDomainEvents<>(member, socialEvent);
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        if (subscription != null) {
            subscription.setMemberInternal(this); // 무한 루프 방지를 위해 내부 메서드 호출
        }
    }

    public void encodePassword(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    public void linkSocialAccount(String provider, String registrationId) {
        this.provider = provider;
        this.registrationId = registrationId;
    }

    public void assignDefaultWorkspace(UUID workspaceId) {
        this.defaultWorkspaceId = workspaceId;
    }
}
