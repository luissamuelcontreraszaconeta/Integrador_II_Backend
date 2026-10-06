package com.exportrace.util;

import com.exportrace.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:ExportRaceSuperSecretKey2026WithAtLeast256BitsForSecurityPass}")
    private String secret;

    // Short-lived Access Token: 15 minutes (900,000 ms) default
    @Value("${jwt.access-token.expiration:900000}")
    private long accessTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        return generateAccessToken(email, role, null, null);
    }

    public String generateAccessToken(User user, String sessionId) {
        String roleName = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        return generateAccessToken(user.getEmail(), roleName, user.getId(), sessionId);
    }

    public String generateAccessToken(String email, String role, Long userId, String sessionId) {
        var builder = Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration));

        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (sessionId != null) {
            builder.claim("sessionId", sessionId);
        }

        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getSessionIdFromToken(String token) {
        try {
            return getClaims(token).get("sessionId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Number num = getClaims(token).get("userId", Number.class);
            return num != null ? num.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = getClaims(token).getExpiration();
            return expirationDate.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
