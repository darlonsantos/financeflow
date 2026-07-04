package com.financeflow.auth.service;

import com.financeflow.auth.dto.AuthResponse;
import com.financeflow.auth.dto.LoginRequest;
import com.financeflow.auth.dto.RegisterRequest;
import com.financeflow.security.JwtService;
import com.financeflow.security.service.RefreshTokenService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .emailVerified(false)
                .build();

        // Mock HttpServletRequest
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(UUID.class), anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(UUID.class), anyString())).thenReturn("refreshToken");
        when(refreshTokenService.createRefreshToken(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(null);

        // Act
        AuthResponse response = authService.register(request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateAccessToken(any(UUID.class), anyString());
        verify(jwtService).generateRefreshToken(any(UUID.class), anyString());
        verify(refreshTokenService).createRefreshToken(any(UUID.class), anyString(), anyString(), anyString());
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(request, httpServletRequest);
        });

        assertEquals("Email já está em uso", exception.getMessage());
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any(UUID.class), anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(UUID.class), anyString())).thenReturn("refreshToken");
        when(refreshTokenService.createRefreshToken(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(null);

        // Act
        AuthResponse response = authService.login(request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), testUser.getPasswordHash());
        verify(jwtService).generateAccessToken(any(UUID.class), anyString());
        verify(jwtService).generateRefreshToken(any(UUID.class), anyString());
        verify(refreshTokenService).createRefreshToken(any(UUID.class), anyString(), anyString(), anyString());
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request, httpServletRequest);
        });

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request, httpServletRequest);
        });

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), testUser.getPasswordHash());
        verify(jwtService, never()).generateAccessToken(any(UUID.class), anyString());
    }
}
