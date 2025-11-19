package com.synapse.account_service.service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.account_service.domain.ProviderUser;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.entity.Subscription;
import com.synapse.account_service.domain.enums.SubscriptionTier;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.eventuate.publisher.MemberDomainEventPublisher;
import com.synapse.account_service.eventuate.publisher.SubscriptionDomainEventPublisher;
import com.synapse.account_service_api.event.MemberDomainEvent;
import com.synapse.account_service_api.event.SubscriptionDomainEvent;

import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final MemberRepository memberRepository;
    private final MemberDomainEventPublisher memberDomainEventPublisher;
    private final SubscriptionDomainEventPublisher subscriptionDomainEventPublisher;
    private final DefaultWorkspaceService defaultWorkspaceService;

    @Transactional
    public Member registerOauthUser(String provider, ProviderUser providerUser) {
        Optional<Member> memberOptional = memberRepository.findBySocialIdOrEmailOrUsername(
            provider, providerUser.getId(), providerUser.getEmail(), providerUser.getUsername()
        );

        if (memberOptional.isPresent()) {
            Member existingMember = memberOptional.get();
            if (existingMember.getProvider() == null || existingMember.getRegistrationId() == null) {
                existingMember.linkSocialAccount(provider, providerUser.getId());
            }
            return existingMember;
        }

        UUID memberId = UUID.randomUUID();
        UUID defaultWorkspaceId = defaultWorkspaceService.createDefaultWorkspaceId(memberId);

        ResultWithDomainEvents<Member, MemberDomainEvent> memberAndEvents = Member.register(
            memberId,
            providerUser.getEmail(),
            providerUser.getUsername(),
            providerUser.getPassword(),
            provider,
            providerUser.getId()
        );

        Member memberResult = memberAndEvents.result;
        memberResult.assignDefaultWorkspace(defaultWorkspaceId);

        memberRepository.save(memberResult);

        memberDomainEventPublisher.publish(memberResult, memberAndEvents.events);
        publishSubscriptionEvent(memberResult);

        return memberResult;
    }

    private void publishSubscriptionEvent(Member memberResult) {
        ZonedDateTime nextRenewalDate = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1).with(LocalTime.MIDNIGHT);

        ResultWithDomainEvents<Subscription, SubscriptionDomainEvent> subscriptionAndEvents = Subscription.register(
                memberResult.getId(),
                memberResult,
                SubscriptionTier.FREE,
                nextRenewalDate);

        Subscription subscriptionResult = subscriptionAndEvents.result;

        memberResult.setSubscription(subscriptionResult);
        subscriptionDomainEventPublisher.publish(subscriptionResult, subscriptionAndEvents.events);
    }
}
