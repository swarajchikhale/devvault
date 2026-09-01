package com.devvault.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Note entities.
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    /**
     * Finds all notes belonging to a specific user ordered by latest update timestamp.
     *
     * @param userId the owner user's UUID
     * @return list of notes belonging to the user
     */
    List<Note> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Finds a single note by its ID and the owner user's ID.
     *
     * @param id the note UUID
     * @param userId the owner user's UUID
     * @return an Optional containing the Note if found and owned by the user
     */
    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a note exists by its ID and owner user's ID.
     *
     * @param id the note UUID
     * @param userId the owner user's UUID
     * @return true if the note exists and belongs to the user
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
