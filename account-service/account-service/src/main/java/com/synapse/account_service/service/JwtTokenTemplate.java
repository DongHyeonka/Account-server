package com.synapse.account_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.synapse.account_service.configuration.SynapseJwtProperties;
import com.synapse.account_service_api.dto.TokenResult;

@Component
public class JwtTokenTemplate {
    private final Algorithm algorithm;
    private final JWTVerifier accessTokenVerifier;
    private final JWTVerifier refreshTokenVerifier;
    private final SynapseJwtProperties jwtProperties;

    public JwtTokenTemplate(SynapseJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.algorithm = Algorithm.HMAC256(jwtProperties.getSecret());
        this.accessTokenVerifier = JWT.require(this.algorithm)
                .withIssuer(jwtProperties.getIssuer())
                .withAudience(jwtProperties.getAudience().getAccess())
                .build();
        this.refreshTokenVerifier = JWT.require(this.algorithm)
                .withIssuer(jwtProperties.getIssuer())
                .withAudience(jwtProperties.getAudience().getRefresh())
                .build();
    }

    public final TokenResult createToken(String subject, Map<String, ?> claims, Duration ttl, String audience) {
        return builder()
                .subject(subject)
                .claims(claims)
                .ttl(ttl)
                .audience(audience)
                .sign();
    }

    public final TokenResult createRefreshToken(String subject, Duration ttl, String audience, String jwtId) {
        return builder()
                .subject(subject)
                .ttl(ttl)
                .audience(audience)
                .jwtId(jwtId)
                .sign();
    }

    public TokenBuilder builder() {
        return new TokenBuilder();
    }

    public final DecodedJWT verifyAccessToken(String token) throws JWTVerificationException {
        return accessTokenVerifier.verify(token);
    }

    public final DecodedJWT verifyRefreshToken(String token) throws JWTVerificationException {
        return refreshTokenVerifier.verify(token);
    }

    // 테스트용 메서드
    public final String createExpiredTokenForTest(String subject) {
        Instant now = Instant.now();
        Instant past = now.minus(Duration.ofMinutes(10)); // 10분 전 만료

        return JWT.create()
                .withSubject(subject)
                .withIssuedAt(past)
                .withExpiresAt(past)
                .sign(algorithm);
    }

    public final class TokenBuilder {
        private String issuer = jwtProperties.getIssuer();
        private String audience;
        private String subject;
        private Map<String, ?> claims = Collections.emptyMap();
        private Duration ttl;
        private Instant issuedAt = Instant.now();
        private String jwtId;

        public TokenBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public TokenBuilder audience(String audience) {
            this.audience = audience;
            return this;
        }

        public TokenBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public TokenBuilder claims(Map<String, ?> claims) {
            this.claims = claims == null ? Collections.emptyMap() : claims;
            return this;
        }

        public TokenBuilder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public TokenBuilder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public TokenBuilder jwtId(String jwtId) {
            this.jwtId = jwtId;
            return this;
        }

        public TokenResult sign() {
            if (subject == null || audience == null || ttl == null) {
                throw new IllegalStateException("Subject, audience, and ttl must be provided to sign a token.");
            }

            Instant expiration = issuedAt.plus(ttl);
            var jwtBuilder = JWT.create()
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .withSubject(subject)
                    .withIssuedAt(issuedAt)
                    .withExpiresAt(expiration);

            if (jwtId != null && !jwtId.isBlank()) {
                jwtBuilder.withJWTId(jwtId);
            }

            if (!claims.isEmpty()) {
                jwtBuilder.withPayload(claims);
            }

            String token = jwtBuilder.sign(algorithm);
            return new TokenResult(token, expiration);
        }
    }
}
