package it.lsimpresa.manager.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService {
    public static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);

    private Algorithm hmac512;
    private JWTVerifier verifier;
    private JWTVerifier refreshVerifier;
    private final String issuer = "app";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        this.hmac512 = Algorithm.HMAC512(jwtSecret);
        this.verifier = JWT.require(hmac512)
                .withIssuer(issuer)
                .withClaim("type", "access")
                .build();
        this.refreshVerifier = JWT.require(hmac512)
                .withIssuer(issuer)
                .withClaim("type", "refresh")
                .build();
    }

    public String generateAccessToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withArrayClaim("roles", AuthorityUtils.authorityListToSet(userDetails.getAuthorities()).toArray(new String[0]))
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(ACCESS_TOKEN_VALIDITY.toMillis()))
                .withClaim("type", "access")
                .sign(hmac512);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(REFRESH_TOKEN_VALIDITY.toMillis()))
                .withClaim("type", "refresh")
                .sign(hmac512);
    }

    public String validateAccessTokenAndGetUsername(String token) {
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getSubject();
    }

    public String validateRefreshTokenAndGetUsername(String token) {
        DecodedJWT decoded = refreshVerifier.verify(token);
        return decoded.getSubject();
    }
}
