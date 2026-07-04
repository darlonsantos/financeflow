package com.financeflow.auth.service;

import com.financeflow.auth.domain.EmailVerificationToken;
import com.financeflow.auth.repository.EmailVerificationTokenRepository;
import com.financeflow.email.EmailService;
import com.financeflow.email.EmailProperties;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (user.getEmailVerified()) {
            log.info("Email already verified", Map.of("userId", userId));
            return;
        }
        
        // Invalidar tokens anteriores não usados
        tokenRepository.findByUserIdAndUsedFalse(userId)
            .ifPresent(token -> {
                token.setUsed(true);
                tokenRepository.save(token);
            });
        
        // Gerar novo token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusHours(emailProperties.getEmailVerification().getExpirationHours());
        
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .userId(userId)
            .token(token)
            .expiresAt(expiresAt)
            .used(false)
            .build();
        
        tokenRepository.save(verificationToken);
        
        // Enviar email
        String verificationUrl = emailProperties.getFrontendUrl() + "/verify-email?token=" + token;
        try {
            emailService.sendEmailVerificationEmail(user.getEmail(), token, user.getName());
            log.info("Email verification sent", Map.of("userId", userId, "email", user.getEmail()));
        } catch (Exception e) {
            log.error("Error sending email verification", Map.of("userId", userId, "error", e.getMessage()), e);
            // Em desenvolvimento, logar o link para facilitar testes sem SMTP
            if (!java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                log.warn("DEV: Link de verificação (SMTP falhou): {}", verificationUrl);
            }
            throw new RuntimeException("Erro ao enviar e-mail de verificação. Verifique a configuração SMTP (MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD) no servidor.");
        }
    }
    
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido ou expirado"));
        
        if (!verificationToken.isValid()) {
            throw new RuntimeException("Token inválido ou expirado");
        }
        
        User user = userRepository.findById(verificationToken.getUserId())
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Verificar email
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Marcar token como usado
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        log.info("Email verified", Map.of("userId", user.getId(), "email", user.getEmail()));
    }
    
    @Transactional
    public void resendVerificationEmail(UUID userId) {
        sendVerificationEmail(userId);
    }
    
    public UUID getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return user.getId();
    }
    
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
