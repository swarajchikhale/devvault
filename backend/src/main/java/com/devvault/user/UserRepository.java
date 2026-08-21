package com.devvault.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by either their username or email address.
     * Useful for login flows where users can log in using either handle.
     *
     * @param username the username to check
     * @param email the email address to check
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if a user with the specified email address exists.
     *
     * @param email the email address to verify
     * @return true if a record exists with the email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user with the specified username exists.
     *
     * @param username the username to verify
     * @return true if a record exists with the username, false otherwise
     */
    boolean existsByUsername(String username);
}
