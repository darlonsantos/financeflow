package com.financeflow.auth.service;

import com.financeflow.auth.dto.AuthResponse;
import com.financeflow.auth.dto.LoginRequest;
import com.financeflow.auth.dto.RegisterRequest;
import com.financeflow.security.JwtService;
import com.financeflow.security.service.RefreshTokenService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private String getDeviceInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
    
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email já está em uso");
        }
        
        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .emailVerified(false)
            .build();
        
        user = userRepository.save(user);
        
        // Enviar email de verificação
        try {
            emailVerificationService.sendVerificationEmail(user.getId());
        } catch (Exception e) {
            // Log mas não falha o registro
            // O usuário pode solicitar reenvio depois
        }
        
        // Não retornar tokens - usuário precisa verificar email primeiro
        return AuthResponse.builder()
            .accessToken(null)
            .refreshToken(null)
            .email(user.getEmail())
            .name(user.getName())
            .build();
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciais inválidas");
        }
        
        // Verificar se o email foi verificado
        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email não verificado. Por favor, verifique seu email antes de fazer login.");
        }
        
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        
        // Armazenar refresh token
        refreshTokenService.createRefreshToken(
            user.getId(), 
            refreshToken, 
            getDeviceInfo(httpRequest), 
            getClientIp(httpRequest)
        );
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .name(user.getName())
            .build();
    }
    
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // Validar token JWT
        if (!jwtService.validateToken(refreshTokenString, true)) {
            throw new RuntimeException("Refresh token inválido");
        }
        
        // Verificar se existe no banco e está válido
        var refreshToken = refreshTokenService.findByToken(refreshTokenString);
        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token expirado ou revogado");
        }
        
        // Atualizar último uso
        refreshTokenService.updateLastUsed(refreshTokenString);
        
        // Gerar novos tokens
        UUID userId = jwtService.getUserIdFromToken(refreshTokenString, true);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        
        // Revogar token antigo e criar novo
        refreshTokenService.revokeToken(refreshTokenString);
        refreshTokenService.createRefreshToken(
            userId, 
            newRefreshToken, 
            refreshToken.getDeviceInfo(), 
            refreshToken.getIpAddress()
        );
        
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .email(user.getEmail())
            .name(user.getName())
            .build();
    }
    
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }
    
    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
