package com.devvault.auth;

import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.dto.UserResponse;
import com.devvault.auth.exception.DuplicateEmailException;
import com.devvault.auth.exception.DuplicateUsernameException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication and registration business logic.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection for UserRepository and PasswordEncoder.
     *
     * @param userRepository the UserRepository to inject
     * @param passwordEncoder the PasswordEncoder bean to inject
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
