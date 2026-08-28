package com.devvault.auth;

import com.devvault.auth.dto.LoginRequest;
import com.devvault.auth.dto.LoginResponse;
import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.dto.UserResponse;
import com.devvault.auth.exception.DuplicateEmailException;
import com.devvault.auth.exception.DuplicateUsernameException;
import com.devvault.auth.exception.InvalidCredentialsException;
import com.devvault.auth.jwt.JwtService;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, and login business logic.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor injection for UserRepository, PasswordEncoder, and JwtService.
     *
     * @param userRepository the UserRepository to inject
     * @param passwordEncoder the PasswordEncoder bean to inject
     * @param jwtService the JwtService to inject
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration details
     * @return a sanitized UserResponse representing the created account
     * @throws DuplicateUsernameException if the username is already registered
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email '" + request.getEmail() + "' is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(request.getUsername(), request.getEmail(), passwordHash);
        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }

    /**
     * Authenticates a user by username or email identifier and password.
     *
     * @param request the login credentials
     * @return a LoginResponse containing the generated JWT access token and token type
     * @throws InvalidCredentialsException if the user is not found or password is incorrect
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(
                request.getIdentifier(),
                request.getIdentifier()
        ).orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId());
        return new LoginResponse(token, "Bearer");
    }
}
