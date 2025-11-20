package com.synapse.account_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {
    private boolean secure;
    private String sameSite;
    private boolean httpOnly;
    private String path;
    private String domain;
    private long maxAge;
}
