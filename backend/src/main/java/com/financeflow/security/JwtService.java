package com.financeflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {
    
    private final JwtProperties jwtProperties;
    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.accessTokenKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(
            jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(UUID userId, String email) {
        return generateToken(userId, email, accessTokenKey, jwtProperties.getAccessTokenExpiration());
    }
    
    public String generateRefreshToken(UUID userId, String email) {
        return generateToken(userId, email, refreshTokenKey, jwtProperties.getRefreshTokenExpiration());
    }
    
    private String generateToken(UUID userId, String email, SecretKey key, Long expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }
    
    public UUID getUserIdFromToken(String token, boolean isRefresh) {
        return UUID.fromString(getClaimFromToken(token, isRefresh, Claims::getSubject));
    }
    
    public String getEmailFromToken(String token, boolean isRefresh) {
        return getClaimFromToken(token, isRefresh, claims -> claims.get("email", String.class));
    }
    
    public Date getExpirationDateFromToken(String token, boolean isRefresh) {
        return getClaimFromToken(token, isRefresh, Claims::getExpiration);
    }
    
    public <T> T getClaimFromToken(String token, boolean isRefresh, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token, isRefresh);
        return claimsResolver.apply(claims);
    }
    
    private Claims getAllClaimsFromToken(String token, boolean isRefresh) {
        SecretKey key = isRefresh ? refreshTokenKey : accessTokenKey;
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    public Boolean isTokenExpired(String token, boolean isRefresh) {
        Date expiration = getExpirationDateFromToken(token, isRefresh);
        return expiration.before(new Date());
    }
    
    public Boolean validateToken(String token, boolean isRefresh) {
        try {
            return !isTokenExpired(token, isRefresh);
        } catch (Exception e) {
            return false;
        }
    }
}
