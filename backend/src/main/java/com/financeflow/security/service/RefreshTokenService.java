package com.financeflow.security.service;

import com.financeflow.security.domain.RefreshToken;
import com.financeflow.security.repository.RefreshTokenRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    @Value("${jwt.refresh-token-expiration:604800000}") // 7 dias em ms
    private Long refreshTokenExpirationMs;
    
    @Transactional
    public RefreshToken createRefreshToken(UUID userId, String token, String deviceInfo, String ipAddress) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusSeconds(refreshTokenExpirationMs / 1000);
        
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(expiresAt)
            .deviceInfo(deviceInfo)
            .ipAddress(ipAddress)
            .revoked(false)
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        String tokenHash = hashToken(token);
        return refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new RuntimeException("Refresh token não encontrado"));
    }
    
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
    
    @Transactional
    public void updateLastUsed(String token) {
        RefreshToken refreshToken = findByToken(token);
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Executa diariamente às 2h
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpeza de tokens expirados");
        refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
        log.info("Limpeza de tokens expirados concluída");
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash do token", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
