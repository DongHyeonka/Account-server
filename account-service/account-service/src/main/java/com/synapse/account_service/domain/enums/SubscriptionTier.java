package com.synapse.account_service.domain.enums;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SubscriptionTier {
    FREE("free", 3),
    PRO("pro", 100)
    ;

    private final String gatewayValue;
    private final int maxSubscriptionCount;

    SubscriptionTier(String gatewayValue, int maxSubscriptionCount) {
        this.gatewayValue = gatewayValue;
        this.maxSubscriptionCount = maxSubscriptionCount;
    }

    public String getGatewayValue() {
        return gatewayValue;
    }

    public int getMaxSubscriptionCount() {
        return maxSubscriptionCount;
    }

    private static final Map<String, SubscriptionTier> TIER_MAP = Collections.unmodifiableMap(
        Stream.of(values())
            .collect(Collectors.toMap(SubscriptionTier::getGatewayValue, Function.identity()))
    );

    public static SubscriptionTier fromGatewayValue(String tierValue) {
        SubscriptionTier result = TIER_MAP.get(tierValue);
        if(result == null) {
            throw new IllegalArgumentException("일치하는 티어 타입이 없습니다. value=" + tierValue);
        }
        return result;
    }
}
