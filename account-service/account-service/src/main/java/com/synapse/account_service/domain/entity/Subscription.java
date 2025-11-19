package com.synapse.account_service.domain.entity;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.synapse.account_service.domain.common.BaseTimeEntity;
import com.synapse.account_service.domain.enums.SubscriptionTier;
import com.synapse.account_service_api.event.SubscriptionCreatedEvent;
import com.synapse.account_service_api.event.SubscriptionDomainEvent;

import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subscription")
public class Subscription extends BaseTimeEntity {

    private static final DateTimeFormatter FORMATTING_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @Column(name = "subscription_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private SubscriptionTier tier = SubscriptionTier.FREE;

    @Column(name = "next_renewal_date", nullable = false)
    private ZonedDateTime nextRenewalDate;

    @Builder
    public Subscription(UUID id, Member member, SubscriptionTier tier, ZonedDateTime nextRenewalDate) {
        this.id = id;
        this.member = member;
        this.tier = tier;
        this.nextRenewalDate = nextRenewalDate;
    }

    public static ResultWithDomainEvents<Subscription, SubscriptionDomainEvent> register(
        UUID userId, Member member, SubscriptionTier tier, ZonedDateTime nextRenewalDate
    ) {
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .member(member)
                .tier(tier)
                .nextRenewalDate(nextRenewalDate)
                .build();

        SubscriptionCreatedEvent subscriptionEvent = new SubscriptionCreatedEvent(
            subscription.getId(), 
            userId, 
            tier.getGatewayValue(), 
            tier.getMaxSubscriptionCount(),
            nextRenewalDate.format(FORMATTING_PATTERN)
        );

        return new ResultWithDomainEvents<>(subscription, subscriptionEvent);
    }

    protected void setMemberInternal(Member member) {
        this.member = member;
    }
}
