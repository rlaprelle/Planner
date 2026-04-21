package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User", "America/New_York");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateAccessToken(anyString(), any(AppUser.Role.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), any(AppUser.Role.class))).thenReturn("refresh-token");

        authService.register(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void register_defaultsTimezoneToUtc_whenTimezoneIsBlank() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User", null);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(jwtService.generateAccessToken(anyString(), any(AppUser.Role.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), any(AppUser.Role.class))).thenReturn("refresh-token");

        authService.register(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTimezone()).isEqualTo("UTC");
    }

    @Test
    void register_throwsEmailAlreadyTakenException_whenEmailExists() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123", "Test User", "UTC");
        AppUser existingUser = new AppUser("taken@example.com", "hash", "Existing", "UTC");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthService.EmailAlreadyTakenException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    void register_returnsAuthResultWithAccessTokenAndRefreshCookie() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User", "UTC");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(jwtService.generateAccessToken("user@example.com", AppUser.Role.USER)).thenReturn("the-access-token");
        when(jwtService.generateRefreshToken("user@example.com", AppUser.Role.USER)).thenReturn("the-refresh-token");

        AuthService.AuthResult result = authService.register(request);

        assertThat(result.authResponse().accessToken()).isEqualTo("the-access-token");
        assertThat(result.refreshCookie().getName()).isEqualTo(AuthService.REFRESH_COOKIE_NAME);
        assertThat(result.refreshCookie().getValue()).isEqualTo("the-refresh-token");
    }
}
