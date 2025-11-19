package com.synapse.account_service.domain.enums;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum MemberRole {
    USER("user"),
    ADMIN("admin");

    private final String gatewayValue;

    MemberRole(String gatewayValue) {
        this.gatewayValue = gatewayValue;
    }

    public List<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.name()));
    }

    public String getGatewayValue() {
        return gatewayValue;
    }

    public static MemberRole fromGatewayValue(String gatewayValue) {
        return Arrays.stream(values())
            .filter(role -> role.gatewayValue.equalsIgnoreCase(gatewayValue))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("일치하는 역할이 없습니다. value=" + gatewayValue));
    }
}
