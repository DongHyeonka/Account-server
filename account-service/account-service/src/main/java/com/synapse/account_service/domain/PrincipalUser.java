package com.synapse.account_service.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.synapse.account_service.domain.entity.Member;

public record PrincipalUser(ProviderUser providerUser, Member member) implements UserDetails, OidcUser {

    public PrincipalUser(ProviderUser providerUser) {
        this(providerUser, null);
    }

    @Override
    public String getName() {
        return providerUser != null ? providerUser.getUsername() : member.getUsername();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return providerUser != null ? providerUser.getAttributes() : Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return providerUser != null ? providerUser.getAuthorities() : member.getRole().getAuthorities();
    }

    @Override
    public String getPassword() {
        return providerUser != null ? providerUser.getPassword() : member.getPassword();
    }

    @Override
    public String getUsername() {
        return providerUser != null ? providerUser.getUsername() : member.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Map<String, Object> getClaims() {
        return providerUser != null ? providerUser.getAttributes() : Collections.emptyMap();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        if (providerUser != null) {
            return new OidcUserInfo(getClaims());
        }
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        // OIDC id_token is not currently used/stored in PrincipalUser
        return null;
    }
}
