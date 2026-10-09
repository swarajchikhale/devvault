package com.devvault.resource;

import com.devvault.resource.dto.CreateResourceRequest;
import com.devvault.resource.dto.ResourceResponse;
import com.devvault.resource.dto.UpdateResourceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for authenticated Resource CRUD operations.
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new IllegalStateException("Authentication principal is missing or invalid");
        }
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Creates a new resource for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param request the resource creation payload
     * @return HTTP 201 Created with the created ResourceResponse
     */
    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(
            Authentication authentication,
            @Valid @RequestBody CreateResourceRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        ResourceResponse response = resourceService.createResource(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all resources owned by the authenticated user.
     *
     * @param authentication the current security authentication
     * @return HTTP 200 OK with list of ResourceResponse objects
     */
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getResources(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        List<ResourceResponse> response = resourceService.getResources(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single resource by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the resource UUID
     * @return HTTP 200 OK with the ResourceResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        ResourceResponse response = resourceService.getResourceById(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing resource by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the resource UUID
     * @param request the update resource payload
     * @return HTTP 200 OK with the updated ResourceResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourceRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        ResourceResponse response = resourceService.updateResource(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a resource by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the resource UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        resourceService.deleteResource(userId, id);
        return ResponseEntity.noContent().build();
    }
}
