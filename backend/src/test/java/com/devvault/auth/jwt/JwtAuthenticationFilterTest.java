package com.devvault.auth.jwt;

import com.devvault.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate when token is valid and user exists in repository")
    void doFilterInternal_ValidToken_UserExists_Authenticates() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.existsById(userId)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate when token is valid but user does not exist in repository")
    void doFilterInternal_ValidToken_UserDoesNotExist_DoesNotAuthenticate() throws ServletException, IOException {
        UUID nonExistentUserId = UUID.randomUUID();
        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(nonExistentUserId);
        when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate when token is invalid")
    void doFilterInternal_InvalidToken_DoesNotAuthenticate() throws ServletException, IOException {
        String token = "invalid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header is missing")
    void doFilterInternal_NoAuthHeader_PassesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
