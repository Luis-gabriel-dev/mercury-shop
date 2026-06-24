package dev.adastratech.mercuryshop.user.adapter.out.security;

import dev.adastratech.mercuryshop.shared.security.SecurityProperties;
import dev.adastratech.mercuryshop.user.domain.AccessToken;
import dev.adastratech.mercuryshop.user.domain.AccessTokenIssuer;
import dev.adastratech.mercuryshop.user.domain.Role;
import dev.adastratech.mercuryshop.user.domain.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emite o access token como JWT RSA. Payload contém apenas {@code sub} (id), {@code roles}
 * e expiração — nada sensível (briefing 7.2).
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;

    JwtAccessTokenIssuer(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = properties.jwt().issuer();
        this.ttl = properties.jwt().accessTokenTtl();
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, ttl.toSeconds());
    }
}
