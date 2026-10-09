package com.devvault.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Resource entities.
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    /**
     * Finds all resources belonging to a specific user ordered by latest update timestamp.
     *
     * @param userId the owner user's UUID
     * @return list of resources belonging to the user
     */
    List<Resource> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Finds a single resource by its ID and the owner user's ID.
     *
     * @param id the resource UUID
     * @param userId the owner user's UUID
     * @return an Optional containing the Resource if found and owned by the user
     */
    Optional<Resource> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a resource exists by its ID and owner user's ID.
     *
     * @param id the resource UUID
     * @param userId the owner user's UUID
     * @return true if the resource exists and belongs to the user
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
