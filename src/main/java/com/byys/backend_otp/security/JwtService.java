package com.byys.backend_otp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.issuer}") String issuer,
        @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact();
    }

    public JwtVerificationResult verify(String token) {
        try {
            var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            String subject = parsed.getPayload().getSubject();
            String tokenIssuer = parsed.getPayload().getIssuer();
            if (!issuer.equals(tokenIssuer)) {
                return new JwtVerificationResult(false, null);
            }
            return new JwtVerificationResult(true, subject);
        } catch (Exception e) {
            return new JwtVerificationResult(false, null);
        }
    }

    public record JwtVerificationResult(boolean valid, String subject) {}
}


