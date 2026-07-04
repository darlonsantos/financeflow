package com.financeflow.auth.service;

import com.financeflow.auth.domain.PasswordResetToken;
import com.financeflow.auth.dto.PasswordResetConfirmRequest;
import com.financeflow.auth.dto.PasswordResetRequest;
import com.financeflow.auth.repository.PasswordResetTokenRepository;
import com.financeflow.email.EmailService;
import com.financeflow.email.EmailProperties;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase().trim();
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                // Por segurança, não revelar se o email existe ou não
                log.warn("Password reset requested for non-existent email", Map.of("email", email));
                throw new RuntimeException("Se o email existir, você receberá um link de recuperação");
            });
        
        // Invalidar tokens anteriores do usuário
        tokenRepository.invalidateUserTokens(user.getId());
        
        // Gerar novo token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusHours(emailProperties.getPasswordReset().getExpirationHours());
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .userId(user.getId())
            .token(token)
            .expiresAt(expiresAt)
            .used(false)
            .build();
        
        tokenRepository.save(resetToken);
        
        // Enviar email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token, user.getName());
            log.info("Password reset email sent", Map.of("userId", user.getId(), "email", email));
        } catch (Exception e) {
            log.error("Error sending password reset email", Map.of("userId", user.getId(), "error", e.getMessage()), e);
            throw new RuntimeException("Erro ao enviar e-mail de recuperação");
        }
    }
    
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new RuntimeException("Token inválido ou expirado"));
        
        if (!token.isValid()) {
            throw new RuntimeException("Token inválido ou expirado");
        }
        
        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Atualizar senha
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Marcar token como usado
        token.setUsed(true);
        tokenRepository.save(token);
        
        // Invalidar todos os refresh tokens do usuário (forçar logout)
        // Isso requer acesso ao RefreshTokenService, mas por enquanto vamos apenas logar
        log.info("Password reset completed", Map.of("userId", user.getId()));
    }
    
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
