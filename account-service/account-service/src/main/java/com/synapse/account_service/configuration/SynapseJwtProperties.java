package com.synapse.account_service.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "synapse.jwt")
public class SynapseJwtProperties {
    private String issuer;
    private Audience audience = new Audience();
    private String secret;
    private long accessTokenTtlMinutes;
    private long refreshTokenTtlDays;

    public Duration getAccessTokenTtl() {
        return Duration.ofMinutes(accessTokenTtlMinutes);
    }

    public Duration getRefreshTokenTtl() {
        return Duration.ofDays(refreshTokenTtlDays);
    }

    @Getter
    @Setter
    public static class Audience {
        private String access;
        private String refresh;
    }
}
