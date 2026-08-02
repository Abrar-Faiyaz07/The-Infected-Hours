package com.infectedhour.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** JWT HS256, 12h expiry (Backend Schema §6). Secret from application.yml, env-overridable. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryHours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiry-hours:12}") long expiryHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiryHours = expiryHours;
    }

    public String generateToken(String playerId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(playerId)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public String extractPlayerId(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirySeconds() {
        return expiryHours * 3600;
    }
}
