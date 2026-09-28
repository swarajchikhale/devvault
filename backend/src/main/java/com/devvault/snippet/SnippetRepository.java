package com.devvault.snippet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Snippet entities.
 */
@Repository
public interface SnippetRepository extends JpaRepository<Snippet, UUID> {

    /**
     * Finds all snippets belonging to a specific user ordered by latest update timestamp.
     *
     * @param userId the owner user's UUID
     * @return list of snippets belonging to the user
     */
    List<Snippet> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Finds a single snippet by its ID and the owner user's ID.
     *
     * @param id the snippet UUID
     * @param userId the owner user's UUID
     * @return an Optional containing the Snippet if found and owned by the user
     */
    Optional<Snippet> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a snippet exists by its ID and owner user's ID.
     *
     * @param id the snippet UUID
     * @param userId the owner user's UUID
     * @return true if the snippet exists and belongs to the user
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
