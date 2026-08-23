package com.devvault.auth;

import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.dto.UserResponse;
import com.devvault.auth.exception.DuplicateEmailException;
import com.devvault.auth.exception.DuplicateUsernameException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should successfully register a new user and return sanitized UserResponse")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("johndoe", "john@example.com", "SecretPass123");

        UUID generatedId = UUID.randomUUID();
        Instant now = Instant.now();

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User passedUser = invocation.getArgument(0);
            return new User(passedUser.getUsername(), passedUser.getEmail(), passedUser.getPasswordHash()) {
                @Override
                public UUID getId() {
                    return generatedId;
                }

                @Override
                public Instant getCreatedAt() {
                    return now;
                }
            };
        });

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(generatedId);
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getCreatedAt()).isEqualTo(now);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("johndoe");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecretPass123");
        assertThat(passwordEncoder.matches("SecretPass123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should throw DuplicateUsernameException when username already exists")
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("johndoe", "john@example.com", "SecretPass123");

        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("Username 'johndoe' is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email already exists")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("johndoe", "john@example.com", "SecretPass123");

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email 'john@example.com' is already registered");

        verify(userRepository, never()).save(any());
    }
}
