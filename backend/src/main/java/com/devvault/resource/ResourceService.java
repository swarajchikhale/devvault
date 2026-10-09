package com.devvault.resource;

import com.devvault.resource.dto.CreateResourceRequest;
import com.devvault.resource.dto.ResourceResponse;
import com.devvault.resource.dto.UpdateResourceRequest;
import com.devvault.resource.exception.ResourceNotFoundException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing resource bookmark creation, retrieval, updates, and deletion with strict user ownership enforcement.
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ResourceService(ResourceRepository resourceRepository, UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new resource for the specified authenticated user.
     *
     * @param userId the owner user's UUID
     * @param request the resource creation payload
     * @return the created ResourceResponse
     */
    @Transactional
    public ResourceResponse createResource(UUID userId, CreateResourceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + userId));

        Resource resource = new Resource(
                user,
                request.getTitle(),
                request.getUrl(),
                request.getDescription(),
                request.getCategory()
        );
        Resource savedResource = resourceRepository.save(resource);

        return ResourceResponse.fromEntity(savedResource);
    }

    /**
     * Retrieves all resources belonging to the authenticated user.
     *
     * @param userId the owner user's UUID
     * @return list of ResourceResponse objects
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> getResources(UUID userId) {
        return resourceRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(ResourceResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single resource by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param resourceId the resource UUID
     * @return the ResourceResponse if found and owned by the user
     * @throws ResourceNotFoundException if the resource is not found or not owned by user
     */
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(UUID userId, UUID resourceId) {
        Resource resource = resourceRepository.findByIdAndUserId(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        return ResourceResponse.fromEntity(resource);
    }

    /**
     * Updates an existing resource by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param resourceId the resource UUID
     * @param request the updated resource payload
     * @return the updated ResourceResponse
     * @throws ResourceNotFoundException if the resource is not found or not owned by user
     */
    @Transactional
    public ResourceResponse updateResource(UUID userId, UUID resourceId, UpdateResourceRequest request) {
        Resource resource = resourceRepository.findByIdAndUserId(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        resource.setTitle(request.getTitle());
        resource.setUrl(request.getUrl());
        resource.setDescription(request.getDescription());
        resource.setCategory(request.getCategory());

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    /**
     * Deletes an existing resource by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param resourceId the resource UUID
     * @throws ResourceNotFoundException if the resource is not found or not owned by user
     */
    @Transactional
    public void deleteResource(UUID userId, UUID resourceId) {
        Resource resource = resourceRepository.findByIdAndUserId(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        resourceRepository.delete(resource);
    }
}
